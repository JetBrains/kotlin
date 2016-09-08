/**
  ******************************************************************************
  * @file    stm32f4xx_pwr.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file provides firmware functions to manage the following 
  *          functionalities of the Power Controller (PWR) peripheral:           
  *           - Backup Domain Access
  *           - PVD configuration
  *           - WakeUp pin configuration
  *           - Main and Backup Regulators configuration
  *           - FLASH Power Down configuration
  *           - Low Power modes configuration
  *           - Flags management
  *               
  ******************************************************************************
  * @attention
  *
  * THE PRESENT FIRMWARE WHICH IS FOR GUIDANCE ONLY AIMS AT PROVIDING CUSTOMERS
  * WITH CODING INFORMATION REGARDING THEIR PRODUCTS IN ORDER FOR THEM TO SAVE
  * TIME. AS A RESULT, STMICROELECTRONICS SHALL NOT BE HELD LIABLE FOR ANY
  * DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES WITH RESPECT TO ANY CLAIMS ARISING
  * FROM THE CONTENT OF SUCH FIRMWARE AND/OR THE USE MADE BY CUSTOMERS OF THE
  * CODING INFORMATION CONTAINED HEREIN IN CONNECTION WITH THEIR PRODUCTS.
  *
  * <h2><center>&copy; COPYRIGHT 2011 STMicroelectronics</center></h2>
  ******************************************************************************
  */ 

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx_conf.h"
#include "stm32f4xx_pwr.h"
#include "stm32f4xx_rcc.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup PWR 
  * @brief PWR driver modules
  * @{
  */ 

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/
/* --------- PWR registers bit address in the alias region ---------- */
#define PWR_OFFSET               (PWR_BASE - PERIPH_BASE)

/* --- CR Register ---*/

/* Alias word address of DBP bit */
#define CR_OFFSET                (PWR_OFFSET + 0x00)
#define DBP_BitNumber            0x08
#define CR_DBP_BB                (PERIPH_BB_BASE + (CR_OFFSET * 32) + (DBP_BitNumber * 4))

/* Alias word address of PVDE bit */
#define PVDE_BitNumber           0x04
#define CR_PVDE_BB               (PERIPH_BB_BASE + (CR_OFFSET * 32) + (PVDE_BitNumber * 4))

/* Alias word address of FPDS bit */
#define FPDS_BitNumber           0x09
#define CR_FPDS_BB               (PERIPH_BB_BASE + (CR_OFFSET * 32) + (FPDS_BitNumber * 4))

/* Alias word address of PMODE bit */
#define PMODE_BitNumber           0x0E
#define CR_PMODE_BB               (PERIPH_BB_BASE + (CR_OFFSET * 32) + (PMODE_BitNumber * 4))


/* --- CSR Register ---*/

/* Alias word address of EWUP bit */
#define CSR_OFFSET               (PWR_OFFSET + 0x04)
#define EWUP_BitNumber           0x08
#define CSR_EWUP_BB              (PERIPH_BB_BASE + (CSR_OFFSET * 32) + (EWUP_BitNumber * 4))

/* Alias word address of BRE bit */
#define BRE_BitNumber            0x09
#define CSR_BRE_BB              (PERIPH_BB_BASE + (CSR_OFFSET * 32) + (BRE_BitNumber * 4))

/* ------------------ PWR registers bit mask ------------------------ */

/* CR register bit mask */
#define CR_DS_MASK               ((uint32_t)0xFFFFFFFC)
#define CR_PLS_MASK              ((uint32_t)0xFFFFFF1F)

/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
/* Private functions ---------------------------------------------------------*/

/** @defgroup PWR_Private_Functions
  * @{
  */

/** @defgroup PWR_Group1 Backup Domain Access function 
 *  @brief   Backup Domain Access function  
 *
@verbatim   
 ===============================================================================
                            Backup Domain Access function 
 ===============================================================================  

  After reset, the backup domain (RTC registers, RTC backup data 
  registers and backup SRAM) is protected against possible unwanted 
  write accesses. 
  To enable access to the RTC Domain and RTC registers, proceed as follows:
    - Enable the Power Controller (PWR) APB1 interface clock using the
      RCC_APB1PeriphClockCmd() function.
    - Enable access to RTC domain using the PWR_BackupAccessCmd() function.

@endverbatim
  * @{
  */

/**
  * @brief  Deinitializes the PWR peripheral registers to their default reset values.     
  * @param  None
  * @retval None
  */
void PWR_DeInit(void)
{
  RCC_APB1PeriphResetCmd(RCC_APB1Periph_PWR, ENABLE);
  RCC_APB1PeriphResetCmd(RCC_APB1Periph_PWR, DISABLE);
}

/**
  * @brief  Enables or disables access to the backup domain (RTC registers, RTC 
  *         backup data registers and backup SRAM).
  * @note   If the HSE divided by 2, 3, ..31 is used as the RTC clock, the 
  *         Backup Domain Access should be kept enabled.
  * @param  NewState: new state of the access to the backup domain.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void PWR_BackupAccessCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  *(__IO uint32_t *) CR_DBP_BB = (uint32_t)NewState;
}

/**
  * @}
  */

/** @defgroup PWR_Group2 PVD configuration functions
 *  @brief   PVD configuration functions 
 *
@verbatim   
 ===============================================================================
                           PVD configuration functions
 ===============================================================================  

 - The PVD is used to monitor the VDD power supply by comparing it to a threshold
   selected by the PVD Level (PLS[2:0] bits in the PWR_CR).
 - A PVDO flag is available to indicate if VDD/VDDA is higher or lower than the 
   PVD threshold. This event is internally connected to the EXTI line16
   and can generate an interrupt if enabled through the EXTI registers.
 - The PVD is stopped in Standby mode.

@endverbatim
  * @{
  */

/**
  * @brief  Configures the voltage threshold detected by the Power Voltage Detector(PVD).
  * @param  PWR_PVDLevel: specifies the PVD detection level
  *          This parameter can be one of the following values:
  *            @arg PWR_PVDLevel_0: PVD detection level set to 2.0V
  *            @arg PWR_PVDLevel_1: PVD detection level set to 2.2V
  *            @arg PWR_PVDLevel_2: PVD detection level set to 2.3V
  *            @arg PWR_PVDLevel_3: PVD detection level set to 2.5V
  *            @arg PWR_PVDLevel_4: PVD detection level set to 2.7V
  *            @arg PWR_PVDLevel_5: PVD detection level set to 2.8V
  *            @arg PWR_PVDLevel_6: PVD detection level set to 2.9V
  *            @arg PWR_PVDLevel_7: PVD detection level set to 3.0V
  * @note   Refer to the electrical characteristics of you device datasheet for more details. 
  * @retval None
  */
void PWR_PVDLevelConfig(uint32_t PWR_PVDLevel)
{
  uint32_t tmpreg = 0;
  
  /* Check the parameters */
  assert_param(IS_PWR_PVD_LEVEL(PWR_PVDLevel));
  
  tmpreg = PWR->CR;
  
  /* Clear PLS[7:5] bits */
  tmpreg &= CR_PLS_MASK;
  
  /* Set PLS[7:5] bits according to PWR_PVDLevel value */
  tmpreg |= PWR_PVDLevel;
  
  /* Store the new value */
  PWR->CR = tmpreg;
}

/**
  * @brief  Enables or disables the Power Voltage Detector(PVD).
  * @param  NewState: new state of the PVD.
  *         This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void PWR_PVDCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  *(__IO uint32_t *) CR_PVDE_BB = (uint32_t)NewState;
}

/**
  * @}
  */

/** @defgroup PWR_Group3 WakeUp pin configuration functions
 *  @brief   WakeUp pin configuration functions 
 *
@verbatim   
 ===============================================================================
                    WakeUp pin configuration functions
 ===============================================================================  

 - WakeUp pin is used to wakeup the system from Standby mode. This pin is 
   forced in input pull down configuration and is active on rising edges.
 - There is only one WakeUp pin: WakeUp Pin 1 on PA.00.

@endverbatim
  * @{
  */

/**
  * @brief  Enables or disables the WakeUp Pin functionality.
  * @param  NewState: new state of the WakeUp Pin functionality.
  *         This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void PWR_WakeUpPinCmd(FunctionalState NewState)
{
  /* Check the parameters */  
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  *(__IO uint32_t *) CSR_EWUP_BB = (uint32_t)NewState;
}

/**
  * @}
  */

/** @defgroup PWR_Group4 Main and Backup Regulators configuration functions
 *  @brief   Main and Backup Regulators configuration functions 
 *
@verbatim   
 ===============================================================================
                    Main and Backup Regulators configuration functions
 ===============================================================================  

 - The backup domain includes 4 Kbytes of backup SRAM accessible only from the 
   CPU, and address in 32-bit, 16-bit or 8-bit mode. Its content is retained 
   even in Standby or VBAT mode when the low power backup regulator is enabled. 
   It can be considered as an internal EEPROM when VBAT is always present.
   You can use the PWR_BackupRegulatorCmd() function to enable the low power
   backup regulator and use the PWR_GetFlagStatus(PWR_FLAG_BRR) to check if it is
   ready or not. 

 - When the backup domain is supplied by VDD (analog switch connected to VDD) 
   the backup SRAM is powered from VDD which replaces the VBAT power supply to 
   save battery life.

 - The backup SRAM is not mass erased by an tamper event. It is read protected 
   to prevent confidential data, such as cryptographic private key, from being 
   accessed. The backup SRAM can be erased only through the Flash interface when
   a protection level change from level 1 to level 0 is requested. 
   Refer to the description of Read protection (RDP) in the Flash programming manual.

 - The main internal regulator can be configured to have a tradeoff between performance
   and power consumption when the device does not operate at the maximum frequency. 
   This is done through PWR_MainRegulatorModeConfig() function which configure VOS bit
   in PWR_CR register: 
      - When this bit is set (Regulator voltage output Scale 1 mode selected) the System
        frequency can go up to 168 MHz. 
      - When this bit is reset (Regulator voltage output Scale 2 mode selected) the System
        frequency can go up to 144 MHz. 
   Refer to the datasheets for more details.
           
@endverbatim
  * @{
  */

/**
  * @brief  Enables or disables the Backup Regulator.
  * @param  NewState: new state of the Backup Regulator.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void PWR_BackupRegulatorCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  *(__IO uint32_t *) CSR_BRE_BB = (uint32_t)NewState;
}

/**
  * @brief  Configures the main internal regulator output voltage.
  * @param  PWR_Regulator_Voltage: specifies the regulator output voltage to achieve
  *         a tradeoff between performance and power consumption when the device does
  *         not operate at the maximum frequency (refer to the datasheets for more details).
  *          This parameter can be one of the following values:
  *            @arg PWR_Regulator_Voltage_Scale1: Regulator voltage output Scale 1 mode, 
  *                                                System frequency up to 168 MHz. 
  *            @arg PWR_Regulator_Voltage_Scale2: Regulator voltage output Scale 2 mode, 
  *                                                System frequency up to 144 MHz.    
  * @retval None
  */
void PWR_MainRegulatorModeConfig(uint32_t PWR_Regulator_Voltage)
{
  /* Check the parameters */
  assert_param(IS_PWR_REGULATOR_VOLTAGE(PWR_Regulator_Voltage));

  if (PWR_Regulator_Voltage == PWR_Regulator_Voltage_Scale2)
  {
    PWR->CR &= ~PWR_Regulator_Voltage_Scale1;
  }
  else
  {    
    PWR->CR |= PWR_Regulator_Voltage_Scale1;
  }
}

/**
  * @}
  */

/** @defgroup PWR_Group5 FLASH Power Down configuration functions
 *  @brief   FLASH Power Down configuration functions 
 *
@verbatim   
 ===============================================================================
           FLASH Power Down configuration functions
 ===============================================================================  

 - By setting the FPDS bit in the PWR_CR register by using the PWR_FlashPowerDownCmd()
   function, the Flash memory also enters power down mode when the device enters 
   Stop mode. When the Flash memory is in power down mode, an additional startup 
   delay is incurred when waking up from Stop mode.

@endverbatim
  * @{
  */

/**
  * @brief  Enables or disables the Flash Power Down in STOP mode.
  * @param  NewState: new state of the Flash power mode.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void PWR_FlashPowerDownCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  *(__IO uint32_t *) CR_FPDS_BB = (uint32_t)NewState;
}

/**
  * @}
  */

/** @defgroup PWR_Group6 Low Power modes configuration functions
 *  @brief   Low Power modes configuration functions 
 *
@verbatim   
 ===============================================================================
                    Low Power modes configuration functions
 ===============================================================================  

  The devices feature 3 low-power modes:
   - Sleep mode: Cortex-M4 core stopped, peripherals kept running.
   - Stop mode: all clocks are stopped, regulator running, regulator in low power mode
   - Standby mode: 1.2V domain powered off.
   
   Sleep mode
   ===========
    - Entry:
      - The Sleep mode is entered by using the __WFI() or __WFE() functions.
    - Exit:
      - Any peripheral interrupt acknowledged by the nested vectored interrupt 
        controller (NVIC) can wake up the device from Sleep mode.

   Stop mode
   ==========
   In Stop mode, all clocks in the 1.2V domain are stopped, the PLL, the HSI,
   and the HSE RC oscillators are disabled. Internal SRAM and register contents 
   are preserved.
   The voltage regulator can be configured either in normal or low-power mode.
   To minimize the consumption In Stop mode, FLASH can be powered off before 
   entering the Stop mode. It can be switched on again by software after exiting 
   the Stop mode using the PWR_FlashPowerDownCmd() function. 
   
    - Entry:
      - The Stop mode is entered using the PWR_EnterSTOPMode(PWR_Regulator_LowPower,) 
        function with regulator in LowPower or with Regulator ON.
    - Exit:
      - Any EXTI Line (Internal or External) configured in Interrupt/Event mode.
      
   Standby mode
   ============
   The Standby mode allows to achieve the lowest power consumption. It is based 
   on the Cortex-M4 deepsleep mode, with the voltage regulator disabled. 
   The 1.2V domain is consequently powered off. The PLL, the HSI oscillator and 
   the HSE oscillator are also switched off. SRAM and register contents are lost 
   except for the RTC registers, RTC backup registers, backup SRAM and Standby 
   circuitry.
   
   The voltage regulator is OFF.
      
    - Entry:
      - The Standby mode is entered using the PWR_EnterSTANDBYMode() function.
    - Exit:
      - WKUP pin rising edge, RTC alarm (Alarm A and Alarm B), RTC wakeup,
        tamper event, time-stamp event, external reset in NRST pin, IWDG reset.              

   Auto-wakeup (AWU) from low-power mode
   =====================================
   The MCU can be woken up from low-power mode by an RTC Alarm event, an RTC 
   Wakeup event, a tamper event, a time-stamp event, or a comparator event, 
   without depending on an external interrupt (Auto-wakeup mode).

   - RTC auto-wakeup (AWU) from the Stop mode
     ----------------------------------------
     
     - To wake up from the Stop mode with an RTC alarm event, it is necessary to:
       - Configure the EXTI Line 17 to be sensitive to rising edges (Interrupt 
         or Event modes) using the EXTI_Init() function.
       - Enable the RTC Alarm Interrupt using the RTC_ITConfig() function
       - Configure the RTC to generate the RTC alarm using the RTC_SetAlarm() 
         and RTC_AlarmCmd() functions.
     - To wake up from the Stop mode with an RTC Tamper or time stamp event, it 
       is necessary to:
       - Configure the EXTI Line 21 to be sensitive to rising edges (Interrupt 
         or Event modes) using the EXTI_Init() function.
       - Enable the RTC Tamper or time stamp Interrupt using the RTC_ITConfig() 
         function
       - Configure the RTC to detect the tamper or time stamp event using the
         RTC_TimeStampConfig(), RTC_TamperTriggerConfig() and RTC_TamperCmd()
         functions.
     - To wake up from the Stop mode with an RTC WakeUp event, it is necessary to:
       - Configure the EXTI Line 22 to be sensitive to rising edges (Interrupt 
         or Event modes) using the EXTI_Init() function.
       - Enable the RTC WakeUp Interrupt using the RTC_ITConfig() function
       - Configure the RTC to generate the RTC WakeUp event using the RTC_WakeUpClockConfig(), 
         RTC_SetWakeUpCounter() and RTC_WakeUpCmd() functions.

   - RTC auto-wakeup (AWU) from the Standby mode
     -------------------------------------------
     - To wake up from the Standby mode with an RTC alarm event, it is necessary to:
       - Enable the RTC Alarm Interrupt using the RTC_ITConfig() function
       - Configure the RTC to generate the RTC alarm using the RTC_SetAlarm() 
         and RTC_AlarmCmd() functions.
     - To wake up from the Standby mode with an RTC Tamper or time stamp event, it 
       is necessary to:
       - Enable the RTC Tamper or time stamp Interrupt using the RTC_ITConfig() 
         function
       - Configure the RTC to detect the tamper or time stamp event using the
         RTC_TimeStampConfig(), RTC_TamperTriggerConfig() and RTC_TamperCmd()
         functions.
     - To wake up from the Standby mode with an RTC WakeUp event, it is necessary to:
       - Enable the RTC WakeUp Interrupt using the RTC_ITConfig() function
       - Configure the RTC to generate the RTC WakeUp event using the RTC_WakeUpClockConfig(), 
         RTC_SetWakeUpCounter() and RTC_WakeUpCmd() functions.

@endverbatim
  * @{
  */

/**
  * @brief  Enters STOP mode.
  *   
  * @note   In Stop mode, all I/O pins keep the same state as in Run mode.
  * @note   When exiting Stop mode by issuing an interrupt or a wakeup event, 
  *         the HSI RC oscillator is selected as system clock.
  * @note   When the voltage regulator operates in low power mode, an additional 
  *         startup delay is incurred when waking up from Stop mode. 
  *         By keeping the internal regulator ON during Stop mode, the consumption 
  *         is higher although the startup time is reduced.           
  *     
  * @param  PWR_Regulator: specifies the regulator state in STOP mode.
  *          This parameter can be one of the following values:
  *            @arg PWR_Regulator_ON: STOP mode with regulator ON
  *            @arg PWR_Regulator_LowPower: STOP mode with regulator in low power mode
  * @param  PWR_STOPEntry: specifies if STOP mode in entered with WFI or WFE instruction.
  *          This parameter can be one of the following values:
  *            @arg PWR_STOPEntry_WFI: enter STOP mode with WFI instruction
  *            @arg PWR_STOPEntry_WFE: enter STOP mode with WFE instruction
  * @retval None
  */
void PWR_EnterSTOPMode(uint32_t PWR_Regulator, uint8_t PWR_STOPEntry)
{
  uint32_t tmpreg = 0;
  
  /* Check the parameters */
  assert_param(IS_PWR_REGULATOR(PWR_Regulator));
  assert_param(IS_PWR_STOP_ENTRY(PWR_STOPEntry));
  
  /* Select the regulator state in STOP mode ---------------------------------*/
  tmpreg = PWR->CR;
  /* Clear PDDS and LPDSR bits */
  tmpreg &= CR_DS_MASK;
  
  /* Set LPDSR bit according to PWR_Regulator value */
  tmpreg |= PWR_Regulator;
  
  /* Store the new value */
  PWR->CR = tmpreg;
  
  /* Set SLEEPDEEP bit of Cortex System Control Register */
  SCB->SCR |= SCB_SCR_SLEEPDEEP_Msk;
  
  /* Select STOP mode entry --------------------------------------------------*/
  if(PWR_STOPEntry == PWR_STOPEntry_WFI)
  {   
    /* Request Wait For Interrupt */
    __WFI();
  }
  else
  {
    /* Request Wait For Event */
    __WFE();
  }
  /* Reset SLEEPDEEP bit of Cortex System Control Register */
  SCB->SCR &= (uint32_t)~((uint32_t)SCB_SCR_SLEEPDEEP_Msk);  
}

/**
  * @brief  Enters STANDBY mode.
  * @note   In Standby mode, all I/O pins are high impedance except for:
  *          - Reset pad (still available) 
  *          - RTC_AF1 pin (PC13) if configured for tamper, time-stamp, RTC 
  *            Alarm out, or RTC clock calibration out.
  *          - RTC_AF2 pin (PI8) if configured for tamper or time-stamp.  
  *          - WKUP pin 1 (PA0) if enabled.       
  * @param  None
  * @retval None
  */
void PWR_EnterSTANDBYMode(void)
{
  /* Clear Wakeup flag */
  PWR->CR |= PWR_CR_CWUF;
  
  /* Select STANDBY mode */
  PWR->CR |= PWR_CR_PDDS;
  
  /* Set SLEEPDEEP bit of Cortex System Control Register */
  SCB->SCR |= SCB_SCR_SLEEPDEEP_Msk;
  
/* This option is used to ensure that store operations are completed */
#if defined ( __CC_ARM   )
  __force_stores();
#endif
  /* Request Wait For Interrupt */
  __WFI();
}

/**
  * @}
  */

/** @defgroup PWR_Group7 Flags management functions
 *  @brief   Flags management functions 
 *
@verbatim   
 ===============================================================================
                           Flags management functions
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Checks whether the specified PWR flag is set or not.
  * @param  PWR_FLAG: specifies the flag to check.
  *          This parameter can be one of the following values:
  *            @arg PWR_FLAG_WU: Wake Up flag. This flag indicates that a wakeup event 
  *                  was received from the WKUP pin or from the RTC alarm (Alarm A 
  *                  or Alarm B), RTC Tamper event, RTC TimeStamp event or RTC Wakeup.
  *                  An additional wakeup event is detected if the WKUP pin is enabled 
  *                  (by setting the EWUP bit) when the WKUP pin level is already high.  
  *            @arg PWR_FLAG_SB: StandBy flag. This flag indicates that the system was
  *                  resumed from StandBy mode.    
  *            @arg PWR_FLAG_PVDO: PVD Output. This flag is valid only if PVD is enabled 
  *                  by the PWR_PVDCmd() function. The PVD is stopped by Standby mode 
  *                  For this reason, this bit is equal to 0 after Standby or reset
  *                  until the PVDE bit is set.
  *            @arg PWR_FLAG_BRR: Backup regulator ready flag. This bit is not reset 
  *                  when the device wakes up from Standby mode or by a system reset 
  *                  or power reset.  
  *            @arg PWR_FLAG_VOSRDY: This flag indicates that the Regulator voltage 
  *                 scaling output selection is ready. 
  * @retval The new state of PWR_FLAG (SET or RESET).
  */
FlagStatus PWR_GetFlagStatus(uint32_t PWR_FLAG)
{
  FlagStatus bitstatus = RESET;
  
  /* Check the parameters */
  assert_param(IS_PWR_GET_FLAG(PWR_FLAG));
  
  if ((PWR->CSR & PWR_FLAG) != (uint32_t)RESET)
  {
    bitstatus = SET;
  }
  else
  {
    bitstatus = RESET;
  }
  /* Return the flag status */
  return bitstatus;
}

/**
  * @brief  Clears the PWR's pending flags.
  * @param  PWR_FLAG: specifies the flag to clear.
  *          This parameter can be one of the following values:
  *            @arg PWR_FLAG_WU: Wake Up flag
  *            @arg PWR_FLAG_SB: StandBy flag
  * @retval None
  */
void PWR_ClearFlag(uint32_t PWR_FLAG)
{
  /* Check the parameters */
  assert_param(IS_PWR_CLEAR_FLAG(PWR_FLAG));
         
  PWR->CR |=  PWR_FLAG << 2;
}

/**
  * @}
  */

/**
  * @}
  */

/**
  * @}
  */

/**
  * @}
  */

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
