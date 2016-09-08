/**
  ******************************************************************************
  * @file    stm32f4xx_rtc.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file provides firmware functions to manage the following 
  *          functionalities of the Real-Time Clock (RTC) peripheral:
  *           - Initialization
  *           - Calendar (Time and Date) configuration
  *           - Alarms (Alarm A and Alarm B) configuration
  *           - WakeUp Timer configuration
  *           - Daylight Saving configuration
  *           - Output pin Configuration
  *           - Coarse digital Calibration configuration
  *           - Smooth digital Calibration configuration
  *           - TimeStamp configuration
  *           - Tampers configuration
  *           - Backup Data Registers configuration
  *           - Shift control synchronisation    
  *           - RTC Tamper and TimeStamp Pins Selection and Output Type Config configuration
  *           - Interrupts and flags management
  *
  *  @verbatim
  *
  *          ===================================================================
  *                               Backup Domain Operating Condition
  *          ===================================================================
  *          The real-time clock (RTC), the RTC backup registers, and the backup 
  *          SRAM (BKP SRAM) can be powered from the VBAT voltage when the main 
  *          VDD supply is powered off.
  *          To retain the content of the RTC backup registers, backup SRAM, 
  *          and supply the RTC when VDD is turned off, VBAT pin can be connected 
  *          to an optional standby voltage supplied by a battery or by another 
  *          source.
  *
  *          To allow the RTC to operate even when the main digital supply (VDD) 
  *          is turned off, the VBAT pin powers the following blocks:
  *            1 - The RTC
  *            2 - The LSE oscillator
  *            3 - The backup SRAM when the low power backup regulator is enabled
  *            4 - PC13 to PC15 I/Os, plus PI8 I/O (when available)
  *
  *          When the backup domain is supplied by VDD (analog switch connected 
  *          to VDD), the following functions are available:
  *            1 - PC14 and PC15 can be used as either GPIO or LSE pins
  *            2 - PC13 can be used as a GPIO or as the RTC_AF1 pin
  *            3 - PI8 can be used as a GPIO or as the RTC_AF2 pin
  *
  *          When the backup domain is supplied by VBAT (analog switch connected 
  *          to VBAT because VDD is not present), the following functions are available:
  *            1 - PC14 and PC15 can be used as LSE pins only
  *            2 - PC13 can be used as the RTC_AF1 pin 
  *            3 - PI8 can be used as the RTC_AF2 pin
  *
  *          ===================================================================
  *                                    Backup Domain Reset
  *          ===================================================================
  *          The backup domain reset sets all RTC registers and the RCC_BDCR 
  *          register to their reset values. The BKPSRAM is not affected by this
  *          reset. The only way of resetting the BKPSRAM is through the Flash 
  *          interface by requesting a protection level change from 1 to 0.
  *          A backup domain reset is generated when one of the following events
  *          occurs:
  *            1 - Software reset, triggered by setting the BDRST bit in the 
  *                RCC Backup domain control register (RCC_BDCR). You can use the
  *                RCC_BackupResetCmd().
  *            2 - VDD or VBAT power on, if both supplies have previously been
  *                powered off.
  *
  *          ===================================================================
  *                                   Backup Domain Access
  *          ===================================================================
  *          After reset, the backup domain (RTC registers, RTC backup data 
  *          registers and backup SRAM) is protected against possible unwanted 
  *          write accesses. 
  *          To enable access to the RTC Domain and RTC registers, proceed as follows:
  *            - Enable the Power Controller (PWR) APB1 interface clock using the
  *              RCC_APB1PeriphClockCmd() function.
  *            - Enable access to RTC domain using the PWR_BackupAccessCmd() function.
  *            - Select the RTC clock source using the RCC_RTCCLKConfig() function.
  *            - Enable RTC Clock using the RCC_RTCCLKCmd() function.
  *
  *          ===================================================================
  *                                   RTC Driver: how to use it
  *          ===================================================================
  *            - Enable the RTC domain access (see description in the section above)
  *            - Configure the RTC Prescaler (Asynchronous and Synchronous) and
  *              RTC hour format using the RTC_Init() function.
  *
  *          Time and Date configuration
  *          ===========================
  *            - To configure the RTC Calendar (Time and Date) use the RTC_SetTime()
  *              and RTC_SetDate() functions.
  *            - To read the RTC Calendar, use the RTC_GetTime() and RTC_GetDate()
  *              functions.
  *            - Use the RTC_DayLightSavingConfig() function to add or sub one
  *              hour to the RTC Calendar.    
  *
  *          Alarm configuration
  *          ===================
  *            - To configure the RTC Alarm use the RTC_SetAlarm() function.
  *            - Enable the selected RTC Alarm using the RTC_AlarmCmd() function
  *            - To read the RTC Alarm, use the RTC_GetAlarm() function.
  *            - To read the RTC alarm SubSecond, use the RTC_GetAlarmSubSecond() function.
  *
  *          RTC Wakeup configuration
  *          ========================
  *            - Configure the RTC Wakeup Clock source use the RTC_WakeUpClockConfig()
  *              function.
  *            - Configure the RTC WakeUp Counter using the RTC_SetWakeUpCounter() 
  *              function  
  *            - Enable the RTC WakeUp using the RTC_WakeUpCmd() function  
  *            - To read the RTC WakeUp Counter register, use the RTC_GetWakeUpCounter() 
  *              function.
  *
  *          Outputs configuration
  *          =====================
  *          The RTC has 2 different outputs:
  *            - AFO_ALARM: this output is used to manage the RTC Alarm A, Alarm B
  *              and WaKeUp signals.          
  *              To output the selected RTC signal on RTC_AF1 pin, use the 
  *              RTC_OutputConfig() function.                
  *            - AFO_CALIB: this output is 512Hz signal or 1Hz .  
  *              To output the RTC Clock on RTC_AF1 pin, use the RTC_CalibOutputCmd()
  *              function.
  *
  *          Smooth digital Calibration configuration
  *          =================================    
  *            - Configure the RTC Original Digital Calibration Value and the corresponding
  *              calibration cycle period (32s,16s and 8s) using the RTC_SmoothCalibConfig() 
  *              function.
  *
  *          Coarse digital Calibration configuration
  *          =================================
  *            - Configure the RTC Coarse Calibration Value and the corresponding
  *              sign using the RTC_CoarseCalibConfig() function.
  *            - Enable the RTC Coarse Calibration using the RTC_CoarseCalibCmd() 
  *              function  
  *
  *          TimeStamp configuration
  *          =======================
  *            - Configure the RTC_AF1 trigger and enables the RTC TimeStamp 
  *              using the RTC_TimeStampCmd() function.
  *            - To read the RTC TimeStamp Time and Date register, use the 
  *              RTC_GetTimeStamp() function.
  *            - To read the RTC TimeStamp SubSecond register, use the 
  *              RTC_GetTimeStampSubSecond() function.
  *            - The TAMPER1 alternate function can be mapped either to RTC_AF1(PC13)
  *              or RTC_AF2 (PI8) depending on the value of TAMP1INSEL bit in 
  *              RTC_TAFCR register. You can use the  RTC_TamperPinSelection()
  *              function to select the corresponding pin.     
  *
  *          Tamper configuration
  *          ====================
  *            - Enable the RTC Tamper using the RTC_TamperCmd() function.
  *             - Configure the Tamper filter count using RTC_TamperFilterConfig()
  *              function. 
  *            - Configure the RTC Tamper trigger Edge or Level according to the Tamper 
  *              filter (if equal to 0 Edge else Level) value using the RTC_TamperConfig() function.
  *            - Configure the Tamper sampling frequency using RTC_TamperSamplingFreqConfig()
  *              function.
  *            - Configure the Tamper precharge or discharge duration using 
  *              RTC_TamperPinsPrechargeDuration() function.
  *            - Enable the Tamper Pull-UP using RTC_TamperPullUpDisableCmd() function.
  *            - Enable the Time stamp on Tamper detection event using  
  *              RTC_TSOnTamperDetecCmd() function.
  *            - The TIMESTAMP alternate function can be mapped to either RTC_AF1 
  *              or RTC_AF2 depending on the value of the TSINSEL bit in the 
  *              RTC_TAFCR register. You can use the  RTC_TimeStampPinSelection()
  *              function to select the corresponding pin. 
  *
  *          Backup Data Registers configuration
  *          ===================================
  *            - To write to the RTC Backup Data registers, use the RTC_WriteBackupRegister()
  *              function.  
  *            - To read the RTC Backup Data registers, use the RTC_ReadBackupRegister()
  *              function.
  * 
  *          ===================================================================
  *                                  RTC and low power modes
  *          ===================================================================
  *           The MCU can be woken up from a low power mode by an RTC alternate 
  *           function.
  *           The RTC alternate functions are the RTC alarms (Alarm A and Alarm B), 
  *           RTC wakeup, RTC tamper event detection and RTC time stamp event detection.
  *           These RTC alternate functions can wake up the system from the Stop 
  *           and Standby lowpower modes.
  *           The system can also wake up from low power modes without depending 
  *           on an external interrupt (Auto-wakeup mode), by using the RTC alarm 
  *           or the RTC wakeup events.
  *           The RTC provides a programmable time base for waking up from the 
  *           Stop or Standby mode at regular intervals.
  *           Wakeup from STOP and Standby modes is possible only when the RTC 
  *           clock source is LSE or LSI.
  *
  *          ===================================================================
  *                            Selection of RTC_AF1 alternate functions
  *          ===================================================================
  *          The RTC_AF1 pin (PC13) can be used for the following purposes:
  *            - AFO_ALARM output
  *            - AFO_CALIB output
  *            - AFI_TAMPER
  *            - AFI_TIMESTAMP
  *
  * +-------------------------------------------------------------------------------------------------------------+
  * |     Pin         |AFO_ALARM |AFO_CALIB |AFI_TAMPER |AFI_TIMESTAMP | TAMP1INSEL |   TSINSEL    |ALARMOUTTYPE  |
  * |  configuration  | ENABLED  | ENABLED  |  ENABLED  |   ENABLED    |TAMPER1 pin |TIMESTAMP pin |  AFO_ALARM   |
  * |  and function   |          |          |           |              | selection  |  selection   |Configuration |
  * |-----------------|----------|----------|-----------|--------------|------------|--------------|--------------|
  * |   Alarm out     |          |          |           |              |    Don't   |     Don't    |              |
  * |   output OD     |     1    |Don't care|Don't care | Don't care   |    care    |     care     |      0       |
  * |-----------------|----------|----------|-----------|--------------|------------|--------------|--------------|
  * |   Alarm out     |          |          |           |              |    Don't   |     Don't    |              |
  * |   output PP     |     1    |Don't care|Don't care | Don't care   |    care    |     care     |      1       |
  * |-----------------|----------|----------|-----------|--------------|------------|--------------|--------------|
  * | Calibration out |          |          |           |              |    Don't   |     Don't    |              |
  * |   output PP     |     0    |    1     |Don't care | Don't care   |    care    |     care     |  Don't care  |
  * |-----------------|----------|----------|-----------|--------------|------------|--------------|--------------|
  * |  TAMPER input   |          |          |           |              |            |     Don't    |              |
  * |   floating      |     0    |    0     |     1     |      0       |      0     |     care     |  Don't care  |
  * |-----------------|----------|----------|-----------|--------------|------------|--------------|--------------|
  * |  TIMESTAMP and  |          |          |           |              |            |              |              |
  * |  TAMPER input   |     0    |    0     |     1     |      1       |      0     |      0       |  Don't care  |
  * |   floating      |          |          |           |              |            |              |              |
  * |-----------------|----------|----------|-----------|--------------|------------|--------------|--------------|
  * | TIMESTAMP input |          |          |           |              |    Don't   |              |              |
  * |    floating     |     0    |    0     |     0     |      1       |    care    |      0       |  Don't care  |
  * |-----------------|----------|----------|-----------|--------------|------------|--------------|--------------|
  * |  Standard GPIO  |     0    |    0     |     0     |      0       | Don't care |  Don't care  |  Don't care  |
  * +-------------------------------------------------------------------------------------------------------------+
  *
  *
  *          ===================================================================
  *                            Selection of RTC_AF2 alternate functions
  *          ===================================================================
  *          The RTC_AF2 pin (PI8) can be used for the following purposes:
  *            - AFI_TAMPER
  *            - AFI_TIMESTAMP
  *
  * +---------------------------------------------------------------------------------------+
  * |     Pin         |AFI_TAMPER |AFI_TIMESTAMP | TAMP1INSEL |   TSINSEL    |ALARMOUTTYPE  |
  * |  configuration  |  ENABLED  |   ENABLED    |TAMPER1 pin |TIMESTAMP pin |  AFO_ALARM   |
  * |  and function   |           |              | selection  |  selection   |Configuration |
  * |-----------------|-----------|--------------|------------|--------------|--------------|
  * |  TAMPER input   |           |              |            |     Don't    |              |
  * |   floating      |     1     |      0       |      1     |     care     |  Don't care  |
  * |-----------------|-----------|--------------|------------|--------------|--------------|
  * |  TIMESTAMP and  |           |              |            |              |              |
  * |  TAMPER input   |     1     |      1       |      1     |      1       |  Don't care  |
  * |   floating      |           |              |            |              |              |
  * |-----------------|-----------|--------------|------------|--------------|--------------|
  * | TIMESTAMP input |           |              |    Don't   |              |              |
  * |    floating     |     0     |      1       |    care    |      1       |  Don't care  |
  * |-----------------|-----------|--------------|------------|--------------|--------------|
  * |  Standard GPIO  |     0     |      0       | Don't care |  Don't care  |  Don't care  |
  * +---------------------------------------------------------------------------------------+
  * 
  *
  *  @endverbatim
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
#include "stm32f4xx_rtc.h"
#include "stm32f4xx_rcc.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup RTC 
  * @brief RTC driver modules
  * @{
  */

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/

/* Masks Definition */
#define RTC_TR_RESERVED_MASK    ((uint32_t)0x007F7F7F)
#define RTC_DR_RESERVED_MASK    ((uint32_t)0x00FFFF3F) 
#define RTC_INIT_MASK           ((uint32_t)0xFFFFFFFF)  
#define RTC_RSF_MASK            ((uint32_t)0xFFFFFF5F)
#define RTC_FLAGS_MASK          ((uint32_t)(RTC_FLAG_TSOVF | RTC_FLAG_TSF | RTC_FLAG_WUTF | \
                                            RTC_FLAG_ALRBF | RTC_FLAG_ALRAF | RTC_FLAG_INITF | \
                                            RTC_FLAG_RSF | RTC_FLAG_INITS | RTC_FLAG_WUTWF | \
                                            RTC_FLAG_ALRBWF | RTC_FLAG_ALRAWF | RTC_FLAG_TAMP1F ))

#define INITMODE_TIMEOUT         ((uint32_t) 0x00010000)
#define SYNCHRO_TIMEOUT          ((uint32_t) 0x00020000)
#define RECALPF_TIMEOUT          ((uint32_t) 0x00020000)
#define SHPF_TIMEOUT             ((uint32_t) 0x00001000)

/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
static uint8_t RTC_ByteToBcd2(uint8_t Value);
static uint8_t RTC_Bcd2ToByte(uint8_t Value);

/* Private functions ---------------------------------------------------------*/

/** @defgroup RTC_Private_Functions
  * @{
  */ 

/** @defgroup RTC_Group1 Initialization and Configuration functions
 *  @brief   Initialization and Configuration functions 
 *
@verbatim   
 ===============================================================================
                 Initialization and Configuration functions
 ===============================================================================

  This section provide functions allowing to initialize and configure the RTC
  Prescaler (Synchronous and Asynchronous), RTC Hour format, disable RTC registers
  Write protection, enter and exit the RTC initialization mode, RTC registers
  synchronization check and reference clock detection enable.
  
  1. The RTC Prescaler is programmed to generate the RTC 1Hz time base. It is
     split into 2 programmable prescalers to minimize power consumption.
     - A 7-bit asynchronous prescaler and A 13-bit synchronous prescaler.
     - When both prescalers are used, it is recommended to configure the asynchronous
       prescaler to a high value to minimize consumption.

  2. All RTC registers are Write protected. Writing to the RTC registers
     is enabled by writing a key into the Write Protection register, RTC_WPR.

  3. To Configure the RTC Calendar, user application should enter initialization
     mode. In this mode, the calendar counter is stopped and its value can be 
     updated. When the initialization sequence is complete, the calendar restarts 
     counting after 4 RTCCLK cycles.

  4. To read the calendar through the shadow registers after Calendar initialization,
     calendar update or after wakeup from low power modes the software must first 
     clear the RSF flag. The software must then wait until it is set again before 
     reading the calendar, which means that the calendar registers have been 
     correctly copied into the RTC_TR and RTC_DR shadow registers.
     The RTC_WaitForSynchro() function implements the above software sequence 
     (RSF clear and RSF check).

@endverbatim
  * @{
  */

/**
  * @brief  Deinitializes the RTC registers to their default reset values.
  * @note   This function doesn't reset the RTC Clock source and RTC Backup Data
  *         registers.       
  * @param  None
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: RTC registers are deinitialized
  *          - ERROR: RTC registers are not deinitialized
  */
ErrorStatus RTC_DeInit(void)
{
  __IO uint32_t wutcounter = 0x00;
  uint32_t wutwfstatus = 0x00;
  ErrorStatus status = ERROR;
  
  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;

  /* Set Initialization mode */
  if (RTC_EnterInitMode() == ERROR)
  {
    status = ERROR;
  }  
  else
  {
    /* Reset TR, DR and CR registers */
    RTC->TR = (uint32_t)0x00000000;
    RTC->DR = (uint32_t)0x00002101;
    /* Reset All CR bits except CR[2:0] */
    RTC->CR &= (uint32_t)0x00000007;
  
    /* Wait till RTC WUTWF flag is set and if Time out is reached exit */
    do
    {
      wutwfstatus = RTC->ISR & RTC_ISR_WUTWF;
      wutcounter++;  
    } while((wutcounter != INITMODE_TIMEOUT) && (wutwfstatus == 0x00));
    
    if ((RTC->ISR & RTC_ISR_WUTWF) == RESET)
    {
      status = ERROR;
    }
    else
    {
      /* Reset all RTC CR register bits */
      RTC->CR &= (uint32_t)0x00000000;
      RTC->WUTR = (uint32_t)0x0000FFFF;
      RTC->PRER = (uint32_t)0x007F00FF;
      RTC->CALIBR = (uint32_t)0x00000000;
      RTC->ALRMAR = (uint32_t)0x00000000;        
      RTC->ALRMBR = (uint32_t)0x00000000;
      
      /* Reset ISR register and exit initialization mode */
      RTC->ISR = (uint32_t)0x00000000;
      
      /* Reset Tamper and alternate functions configuration register */
      RTC->TAFCR = 0x00000000;
  
      if(RTC_WaitForSynchro() == ERROR)
      {
        status = ERROR;
      }
      else
      {
        status = SUCCESS;      
      }
    }
  }
  
  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF;  
  
  return status;
}

/**
  * @brief  Initializes the RTC registers according to the specified parameters 
  *         in RTC_InitStruct.
  * @param  RTC_InitStruct: pointer to a RTC_InitTypeDef structure that contains 
  *         the configuration information for the RTC peripheral.
  * @note   The RTC Prescaler register is write protected and can be written in 
  *         initialization mode only.  
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: RTC registers are initialized
  *          - ERROR: RTC registers are not initialized  
  */
ErrorStatus RTC_Init(RTC_InitTypeDef* RTC_InitStruct)
{
  ErrorStatus status = ERROR;
  
  /* Check the parameters */
  assert_param(IS_RTC_HOUR_FORMAT(RTC_InitStruct->RTC_HourFormat));
  assert_param(IS_RTC_ASYNCH_PREDIV(RTC_InitStruct->RTC_AsynchPrediv));
  assert_param(IS_RTC_SYNCH_PREDIV(RTC_InitStruct->RTC_SynchPrediv));

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;

  /* Set Initialization mode */
  if (RTC_EnterInitMode() == ERROR)
  {
    status = ERROR;
  } 
  else
  {
    /* Clear RTC CR FMT Bit */
    RTC->CR &= ((uint32_t)~(RTC_CR_FMT));
    /* Set RTC_CR register */
    RTC->CR |=  ((uint32_t)(RTC_InitStruct->RTC_HourFormat));
  
    /* Configure the RTC PRER */
    RTC->PRER = (uint32_t)(RTC_InitStruct->RTC_SynchPrediv);
    RTC->PRER |= (uint32_t)(RTC_InitStruct->RTC_AsynchPrediv << 16);

    /* Exit Initialization mode */
    RTC_ExitInitMode();

    status = SUCCESS;    
  }
  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF; 
  
  return status;
}

/**
  * @brief  Fills each RTC_InitStruct member with its default value.
  * @param  RTC_InitStruct: pointer to a RTC_InitTypeDef structure which will be 
  *         initialized.
  * @retval None
  */
void RTC_StructInit(RTC_InitTypeDef* RTC_InitStruct)
{
  /* Initialize the RTC_HourFormat member */
  RTC_InitStruct->RTC_HourFormat = RTC_HourFormat_24;
    
  /* Initialize the RTC_AsynchPrediv member */
  RTC_InitStruct->RTC_AsynchPrediv = (uint32_t)0x7F;

  /* Initialize the RTC_SynchPrediv member */
  RTC_InitStruct->RTC_SynchPrediv = (uint32_t)0xFF; 
}

/**
  * @brief  Enables or disables the RTC registers write protection.
  * @note   All the RTC registers are write protected except for RTC_ISR[13:8], 
  *         RTC_TAFCR and RTC_BKPxR.
  * @note   Writing a wrong key reactivates the write protection.
  * @note   The protection mechanism is not affected by system reset.  
  * @param  NewState: new state of the write protection.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void RTC_WriteProtectionCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
    
  if (NewState != DISABLE)
  {
    /* Enable the write protection for RTC registers */
    RTC->WPR = 0xFF;   
  }
  else
  {
    /* Disable the write protection for RTC registers */
    RTC->WPR = 0xCA;
    RTC->WPR = 0x53;    
  }
}

/**
  * @brief  Enters the RTC Initialization mode.
  * @note   The RTC Initialization mode is write protected, use the 
  *         RTC_WriteProtectionCmd(DISABLE) before calling this function.    
  * @param  None
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: RTC is in Init mode
  *          - ERROR: RTC is not in Init mode  
  */
ErrorStatus RTC_EnterInitMode(void)
{
  __IO uint32_t initcounter = 0x00;
  ErrorStatus status = ERROR;
  uint32_t initstatus = 0x00;
     
  /* Check if the Initialization mode is set */
  if ((RTC->ISR & RTC_ISR_INITF) == (uint32_t)RESET)
  {
    /* Set the Initialization mode */
    RTC->ISR = (uint32_t)RTC_INIT_MASK;
    
    /* Wait till RTC is in INIT state and if Time out is reached exit */
    do
    {
      initstatus = RTC->ISR & RTC_ISR_INITF;
      initcounter++;  
    } while((initcounter != INITMODE_TIMEOUT) && (initstatus == 0x00));
    
    if ((RTC->ISR & RTC_ISR_INITF) != RESET)
    {
      status = SUCCESS;
    }
    else
    {
      status = ERROR;
    }        
  }
  else
  {
    status = SUCCESS;  
  } 
    
  return (status);  
}

/**
  * @brief  Exits the RTC Initialization mode.
  * @note   When the initialization sequence is complete, the calendar restarts 
  *         counting after 4 RTCCLK cycles.  
  * @note   The RTC Initialization mode is write protected, use the 
  *         RTC_WriteProtectionCmd(DISABLE) before calling this function.      
  * @param  None
  * @retval None
  */
void RTC_ExitInitMode(void)
{ 
  /* Exit Initialization mode */
  RTC->ISR &= (uint32_t)~RTC_ISR_INIT;  
}

/**
  * @brief  Waits until the RTC Time and Date registers (RTC_TR and RTC_DR) are 
  *         synchronized with RTC APB clock.
  * @note   The RTC Resynchronization mode is write protected, use the 
  *         RTC_WriteProtectionCmd(DISABLE) before calling this function. 
  * @note   To read the calendar through the shadow registers after Calendar 
  *         initialization, calendar update or after wakeup from low power modes 
  *         the software must first clear the RSF flag. 
  *         The software must then wait until it is set again before reading 
  *         the calendar, which means that the calendar registers have been 
  *         correctly copied into the RTC_TR and RTC_DR shadow registers.   
  * @param  None
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: RTC registers are synchronised
  *          - ERROR: RTC registers are not synchronised
  */
ErrorStatus RTC_WaitForSynchro(void)
{
  __IO uint32_t synchrocounter = 0;
  ErrorStatus status = ERROR;
  uint32_t synchrostatus = 0x00;

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;
    
  /* Clear RSF flag */
  RTC->ISR &= (uint32_t)RTC_RSF_MASK;
    
  /* Wait the registers to be synchronised */
  do
  {
    synchrostatus = RTC->ISR & RTC_ISR_RSF;
    synchrocounter++;  
  } while((synchrocounter != SYNCHRO_TIMEOUT) && (synchrostatus == 0x00));
    
  if ((RTC->ISR & RTC_ISR_RSF) != RESET)
  {
    status = SUCCESS;
  }
  else
  {
    status = ERROR;
  }        

  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF; 
    
  return (status); 
}

/**
  * @brief  Enables or disables the RTC reference clock detection.
  * @param  NewState: new state of the RTC reference clock.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: RTC reference clock detection is enabled
  *          - ERROR: RTC reference clock detection is disabled  
  */
ErrorStatus RTC_RefClockCmd(FunctionalState NewState)
{ 
  ErrorStatus status = ERROR;
  
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;
    
  /* Set Initialization mode */
  if (RTC_EnterInitMode() == ERROR)
  {
    status = ERROR;
  } 
  else
  {  
    if (NewState != DISABLE)
    {
      /* Enable the RTC reference clock detection */
      RTC->CR |= RTC_CR_REFCKON;   
    }
    else
    {
      /* Disable the RTC reference clock detection */
      RTC->CR &= ~RTC_CR_REFCKON;    
    }
    /* Exit Initialization mode */
    RTC_ExitInitMode();
    
    status = SUCCESS;
  }
  
  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF;  
  
  return status; 
}

/**
  * @brief  Enables or Disables the Bypass Shadow feature.
  * @note   When the Bypass Shadow is enabled the calendar value are taken 
  *         directly from the Calendar counter.
  * @param  NewState: new state of the Bypass Shadow feature.
  *         This parameter can be: ENABLE or DISABLE.
  * @retval None
*/
void RTC_BypassShadowCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;
  
  if (NewState != DISABLE)
  {
    /* Set the BYPSHAD bit */
    RTC->CR |= (uint8_t)RTC_CR_BYPSHAD;
  }
  else
  {
    /* Reset the BYPSHAD bit */
    RTC->CR &= (uint8_t)~RTC_CR_BYPSHAD;
  }

  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF;
}

/**
  * @}
  */

/** @defgroup RTC_Group2 Time and Date configuration functions
 *  @brief   Time and Date configuration functions 
 *
@verbatim   
 ===============================================================================
                   Time and Date configuration functions
 ===============================================================================  

  This section provide functions allowing to program and read the RTC Calendar
  (Time and Date).

@endverbatim
  * @{
  */

/**
  * @brief  Set the RTC current time.
  * @param  RTC_Format: specifies the format of the entered parameters.
  *          This parameter can be  one of the following values:
  *            @arg RTC_Format_BIN:  Binary data format 
  *            @arg RTC_Format_BCD:  BCD data format
  * @param  RTC_TimeStruct: pointer to a RTC_TimeTypeDef structure that contains 
  *                        the time configuration information for the RTC.     
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: RTC Time register is configured
  *          - ERROR: RTC Time register is not configured
  */
ErrorStatus RTC_SetTime(uint32_t RTC_Format, RTC_TimeTypeDef* RTC_TimeStruct)
{
  uint32_t tmpreg = 0;
  ErrorStatus status = ERROR;
    
  /* Check the parameters */
  assert_param(IS_RTC_FORMAT(RTC_Format));
  
  if (RTC_Format == RTC_Format_BIN)
  {
    if ((RTC->CR & RTC_CR_FMT) != (uint32_t)RESET)
    {
      assert_param(IS_RTC_HOUR12(RTC_TimeStruct->RTC_Hours));
      assert_param(IS_RTC_H12(RTC_TimeStruct->RTC_H12));
    } 
    else
    {
      RTC_TimeStruct->RTC_H12 = 0x00;
      assert_param(IS_RTC_HOUR24(RTC_TimeStruct->RTC_Hours));
    }
    assert_param(IS_RTC_MINUTES(RTC_TimeStruct->RTC_Minutes));
    assert_param(IS_RTC_SECONDS(RTC_TimeStruct->RTC_Seconds));
  }
  else
  {
    if ((RTC->CR & RTC_CR_FMT) != (uint32_t)RESET)
    {
      tmpreg = RTC_Bcd2ToByte(RTC_TimeStruct->RTC_Hours);
      assert_param(IS_RTC_HOUR12(tmpreg));
      assert_param(IS_RTC_H12(RTC_TimeStruct->RTC_H12)); 
    } 
    else
    {
      RTC_TimeStruct->RTC_H12 = 0x00;
      assert_param(IS_RTC_HOUR24(RTC_Bcd2ToByte(RTC_TimeStruct->RTC_Hours)));
    }
    assert_param(IS_RTC_MINUTES(RTC_Bcd2ToByte(RTC_TimeStruct->RTC_Minutes)));
    assert_param(IS_RTC_SECONDS(RTC_Bcd2ToByte(RTC_TimeStruct->RTC_Seconds)));
  }
  
  /* Check the input parameters format */
  if (RTC_Format != RTC_Format_BIN)
  {
    tmpreg = (((uint32_t)(RTC_TimeStruct->RTC_Hours) << 16) | \
             ((uint32_t)(RTC_TimeStruct->RTC_Minutes) << 8) | \
             ((uint32_t)RTC_TimeStruct->RTC_Seconds) | \
             ((uint32_t)(RTC_TimeStruct->RTC_H12) << 16)); 
  }  
  else
  {
    tmpreg = (uint32_t)(((uint32_t)RTC_ByteToBcd2(RTC_TimeStruct->RTC_Hours) << 16) | \
                   ((uint32_t)RTC_ByteToBcd2(RTC_TimeStruct->RTC_Minutes) << 8) | \
                   ((uint32_t)RTC_ByteToBcd2(RTC_TimeStruct->RTC_Seconds)) | \
                   (((uint32_t)RTC_TimeStruct->RTC_H12) << 16));
  }  

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;

  /* Set Initialization mode */
  if (RTC_EnterInitMode() == ERROR)
  {
    status = ERROR;
  } 
  else
  {
    /* Set the RTC_TR register */
    RTC->TR = (uint32_t)(tmpreg & RTC_TR_RESERVED_MASK);

    /* Exit Initialization mode */
    RTC_ExitInitMode(); 

    if(RTC_WaitForSynchro() == ERROR)
    {
      status = ERROR;
    }
    else
    {
      status = SUCCESS;
    }
  
  }
  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF; 
    
  return status;
}

/**
  * @brief  Fills each RTC_TimeStruct member with its default value
  *         (Time = 00h:00min:00sec).
  * @param  RTC_TimeStruct: pointer to a RTC_TimeTypeDef structure which will be 
  *         initialized.
  * @retval None
  */
void RTC_TimeStructInit(RTC_TimeTypeDef* RTC_TimeStruct)
{
  /* Time = 00h:00min:00sec */
  RTC_TimeStruct->RTC_H12 = RTC_H12_AM;
  RTC_TimeStruct->RTC_Hours = 0;
  RTC_TimeStruct->RTC_Minutes = 0;
  RTC_TimeStruct->RTC_Seconds = 0; 
}

/**
  * @brief  Get the RTC current Time.
  * @param  RTC_Format: specifies the format of the returned parameters.
  *          This parameter can be  one of the following values:
  *            @arg RTC_Format_BIN:  Binary data format 
  *            @arg RTC_Format_BCD:  BCD data format
  * @param  RTC_TimeStruct: pointer to a RTC_TimeTypeDef structure that will 
  *                        contain the returned current time configuration.     
  * @retval None
  */
void RTC_GetTime(uint32_t RTC_Format, RTC_TimeTypeDef* RTC_TimeStruct)
{
  uint32_t tmpreg = 0;

  /* Check the parameters */
  assert_param(IS_RTC_FORMAT(RTC_Format));

  /* Get the RTC_TR register */
  tmpreg = (uint32_t)(RTC->TR & RTC_TR_RESERVED_MASK); 
  
  /* Fill the structure fields with the read parameters */
  RTC_TimeStruct->RTC_Hours = (uint8_t)((tmpreg & (RTC_TR_HT | RTC_TR_HU)) >> 16);
  RTC_TimeStruct->RTC_Minutes = (uint8_t)((tmpreg & (RTC_TR_MNT | RTC_TR_MNU)) >>8);
  RTC_TimeStruct->RTC_Seconds = (uint8_t)(tmpreg & (RTC_TR_ST | RTC_TR_SU));
  RTC_TimeStruct->RTC_H12 = (uint8_t)((tmpreg & (RTC_TR_PM)) >> 16);  

  /* Check the input parameters format */
  if (RTC_Format == RTC_Format_BIN)
  {
    /* Convert the structure parameters to Binary format */
    RTC_TimeStruct->RTC_Hours = (uint8_t)RTC_Bcd2ToByte(RTC_TimeStruct->RTC_Hours);
    RTC_TimeStruct->RTC_Minutes = (uint8_t)RTC_Bcd2ToByte(RTC_TimeStruct->RTC_Minutes);
    RTC_TimeStruct->RTC_Seconds = (uint8_t)RTC_Bcd2ToByte(RTC_TimeStruct->RTC_Seconds);   
  }
}

/**
  * @brief  Gets the RTC current Calendar Subseconds value.
  * @note   This function freeze the Time and Date registers after reading the 
  *         SSR register.
  * @param  None
  * @retval RTC current Calendar Subseconds value.
  */
uint32_t RTC_GetSubSecond(void)
{
  uint32_t tmpreg = 0;
  
  /* Get subseconds values from the correspondent registers*/
  tmpreg = (uint32_t)(RTC->SSR);
  
  /* Read DR register to unfroze calendar registers */
  (void) (RTC->DR);
  
  return (tmpreg);
}

/**
  * @brief  Set the RTC current date.
  * @param  RTC_Format: specifies the format of the entered parameters.
  *          This parameter can be  one of the following values:
  *            @arg RTC_Format_BIN:  Binary data format 
  *            @arg RTC_Format_BCD:  BCD data format
  * @param  RTC_DateStruct: pointer to a RTC_DateTypeDef structure that contains 
  *                         the date configuration information for the RTC.
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: RTC Date register is configured
  *          - ERROR: RTC Date register is not configured
  */
ErrorStatus RTC_SetDate(uint32_t RTC_Format, RTC_DateTypeDef* RTC_DateStruct)
{
  uint32_t tmpreg = 0;
  ErrorStatus status = ERROR;
  
  /* Check the parameters */
  assert_param(IS_RTC_FORMAT(RTC_Format));

  if ((RTC_Format == RTC_Format_BIN) && ((RTC_DateStruct->RTC_Month & 0x10) == 0x10))
  {
    RTC_DateStruct->RTC_Month = (RTC_DateStruct->RTC_Month & (uint32_t)~(0x10)) + 0x0A;
  }  
  if (RTC_Format == RTC_Format_BIN)
  {
    assert_param(IS_RTC_YEAR(RTC_DateStruct->RTC_Year));
    assert_param(IS_RTC_MONTH(RTC_DateStruct->RTC_Month));
    assert_param(IS_RTC_DATE(RTC_DateStruct->RTC_Date));
  }
  else
  {
    assert_param(IS_RTC_YEAR(RTC_Bcd2ToByte(RTC_DateStruct->RTC_Year)));
    tmpreg = RTC_Bcd2ToByte(RTC_DateStruct->RTC_Month);
    assert_param(IS_RTC_MONTH(tmpreg));
    tmpreg = RTC_Bcd2ToByte(RTC_DateStruct->RTC_Date);
    assert_param(IS_RTC_DATE(tmpreg));
  }
  assert_param(IS_RTC_WEEKDAY(RTC_DateStruct->RTC_WeekDay));

  /* Check the input parameters format */
  if (RTC_Format != RTC_Format_BIN)
  {
    tmpreg = ((((uint32_t)RTC_DateStruct->RTC_Year) << 16) | \
              (((uint32_t)RTC_DateStruct->RTC_Month) << 8) | \
              ((uint32_t)RTC_DateStruct->RTC_Date) | \
              (((uint32_t)RTC_DateStruct->RTC_WeekDay) << 13)); 
  }  
  else
  {
    tmpreg = (((uint32_t)RTC_ByteToBcd2(RTC_DateStruct->RTC_Year) << 16) | \
              ((uint32_t)RTC_ByteToBcd2(RTC_DateStruct->RTC_Month) << 8) | \
              ((uint32_t)RTC_ByteToBcd2(RTC_DateStruct->RTC_Date)) | \
              ((uint32_t)RTC_DateStruct->RTC_WeekDay << 13));
  }

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;

  /* Set Initialization mode */
  if (RTC_EnterInitMode() == ERROR)
  {
    status = ERROR;
  } 
  else
  {
    /* Set the RTC_DR register */
    RTC->DR = (uint32_t)(tmpreg & RTC_DR_RESERVED_MASK);

    /* Exit Initialization mode */
    RTC_ExitInitMode(); 

    if(RTC_WaitForSynchro() == ERROR)
    {
      status = ERROR;
    }
    else
    {
      status = SUCCESS;
    }
  }
  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF;   
  
  return status;
}

/**
  * @brief  Fills each RTC_DateStruct member with its default value
  *         (Monday, January 01 xx00).
  * @param  RTC_DateStruct: pointer to a RTC_DateTypeDef structure which will be 
  *         initialized.
  * @retval None
  */
void RTC_DateStructInit(RTC_DateTypeDef* RTC_DateStruct)
{
  /* Monday, January 01 xx00 */
  RTC_DateStruct->RTC_WeekDay = RTC_Weekday_Monday;
  RTC_DateStruct->RTC_Date = 1;
  RTC_DateStruct->RTC_Month = RTC_Month_January;
  RTC_DateStruct->RTC_Year = 0;
}

/**
  * @brief  Get the RTC current date. 
  * @param  RTC_Format: specifies the format of the returned parameters.
  *          This parameter can be one of the following values:
  *            @arg RTC_Format_BIN: Binary data format 
  *            @arg RTC_Format_BCD: BCD data format
  * @param RTC_DateStruct: pointer to a RTC_DateTypeDef structure that will 
  *                        contain the returned current date configuration.     
  * @retval None
  */
void RTC_GetDate(uint32_t RTC_Format, RTC_DateTypeDef* RTC_DateStruct)
{
  uint32_t tmpreg = 0;

  /* Check the parameters */
  assert_param(IS_RTC_FORMAT(RTC_Format));
  
  /* Get the RTC_TR register */
  tmpreg = (uint32_t)(RTC->DR & RTC_DR_RESERVED_MASK); 

  /* Fill the structure fields with the read parameters */
  RTC_DateStruct->RTC_Year = (uint8_t)((tmpreg & (RTC_DR_YT | RTC_DR_YU)) >> 16);
  RTC_DateStruct->RTC_Month = (uint8_t)((tmpreg & (RTC_DR_MT | RTC_DR_MU)) >> 8);
  RTC_DateStruct->RTC_Date = (uint8_t)(tmpreg & (RTC_DR_DT | RTC_DR_DU));
  RTC_DateStruct->RTC_WeekDay = (uint8_t)((tmpreg & (RTC_DR_WDU)) >> 13);

  /* Check the input parameters format */
  if (RTC_Format == RTC_Format_BIN)
  {
    /* Convert the structure parameters to Binary format */
    RTC_DateStruct->RTC_Year = (uint8_t)RTC_Bcd2ToByte(RTC_DateStruct->RTC_Year);
    RTC_DateStruct->RTC_Month = (uint8_t)RTC_Bcd2ToByte(RTC_DateStruct->RTC_Month);
    RTC_DateStruct->RTC_Date = (uint8_t)RTC_Bcd2ToByte(RTC_DateStruct->RTC_Date);
  }
}

/**
  * @}
  */

/** @defgroup RTC_Group3 Alarms configuration functions
 *  @brief   Alarms (Alarm A and Alarm B) configuration functions 
 *
@verbatim   
 ===============================================================================
              Alarms (Alarm A and Alarm B) configuration functions
 ===============================================================================  

  This section provide functions allowing to program and read the RTC Alarms.

@endverbatim
  * @{
  */

/**
  * @brief  Set the specified RTC Alarm.
  * @note   The Alarm register can only be written when the corresponding Alarm
  *         is disabled (Use the RTC_AlarmCmd(DISABLE)).    
  * @param  RTC_Format: specifies the format of the returned parameters.
  *          This parameter can be one of the following values:
  *            @arg RTC_Format_BIN: Binary data format 
  *            @arg RTC_Format_BCD: BCD data format
  * @param  RTC_Alarm: specifies the alarm to be configured.
  *          This parameter can be one of the following values:
  *            @arg RTC_Alarm_A: to select Alarm A
  *            @arg RTC_Alarm_B: to select Alarm B  
  * @param  RTC_AlarmStruct: pointer to a RTC_AlarmTypeDef structure that 
  *                          contains the alarm configuration parameters.     
  * @retval None
  */
void RTC_SetAlarm(uint32_t RTC_Format, uint32_t RTC_Alarm, RTC_AlarmTypeDef* RTC_AlarmStruct)
{
  uint32_t tmpreg = 0;
  
  /* Check the parameters */
  assert_param(IS_RTC_FORMAT(RTC_Format));
  assert_param(IS_RTC_ALARM(RTC_Alarm));
  assert_param(IS_ALARM_MASK(RTC_AlarmStruct->RTC_AlarmMask));
  assert_param(IS_RTC_ALARM_DATE_WEEKDAY_SEL(RTC_AlarmStruct->RTC_AlarmDateWeekDaySel));

  if (RTC_Format == RTC_Format_BIN)
  {
    if ((RTC->CR & RTC_CR_FMT) != (uint32_t)RESET)
    {
      assert_param(IS_RTC_HOUR12(RTC_AlarmStruct->RTC_AlarmTime.RTC_Hours));
      assert_param(IS_RTC_H12(RTC_AlarmStruct->RTC_AlarmTime.RTC_H12));
    } 
    else
    {
      RTC_AlarmStruct->RTC_AlarmTime.RTC_H12 = 0x00;
      assert_param(IS_RTC_HOUR24(RTC_AlarmStruct->RTC_AlarmTime.RTC_Hours));
    }
    assert_param(IS_RTC_MINUTES(RTC_AlarmStruct->RTC_AlarmTime.RTC_Minutes));
    assert_param(IS_RTC_SECONDS(RTC_AlarmStruct->RTC_AlarmTime.RTC_Seconds));
    
    if(RTC_AlarmStruct->RTC_AlarmDateWeekDaySel == RTC_AlarmDateWeekDaySel_Date)
    {
      assert_param(IS_RTC_ALARM_DATE_WEEKDAY_DATE(RTC_AlarmStruct->RTC_AlarmDateWeekDay));
    }
    else
    {
      assert_param(IS_RTC_ALARM_DATE_WEEKDAY_WEEKDAY(RTC_AlarmStruct->RTC_AlarmDateWeekDay));
    }
  }
  else
  {
    if ((RTC->CR & RTC_CR_FMT) != (uint32_t)RESET)
    {
      tmpreg = RTC_Bcd2ToByte(RTC_AlarmStruct->RTC_AlarmTime.RTC_Hours);
      assert_param(IS_RTC_HOUR12(tmpreg));
      assert_param(IS_RTC_H12(RTC_AlarmStruct->RTC_AlarmTime.RTC_H12));
    } 
    else
    {
      RTC_AlarmStruct->RTC_AlarmTime.RTC_H12 = 0x00;
      assert_param(IS_RTC_HOUR24(RTC_Bcd2ToByte(RTC_AlarmStruct->RTC_AlarmTime.RTC_Hours)));
    }
    
    assert_param(IS_RTC_MINUTES(RTC_Bcd2ToByte(RTC_AlarmStruct->RTC_AlarmTime.RTC_Minutes)));
    assert_param(IS_RTC_SECONDS(RTC_Bcd2ToByte(RTC_AlarmStruct->RTC_AlarmTime.RTC_Seconds)));
    
    if(RTC_AlarmStruct->RTC_AlarmDateWeekDaySel == RTC_AlarmDateWeekDaySel_Date)
    {
      tmpreg = RTC_Bcd2ToByte(RTC_AlarmStruct->RTC_AlarmDateWeekDay);
      assert_param(IS_RTC_ALARM_DATE_WEEKDAY_DATE(tmpreg));    
    }
    else
    {
      tmpreg = RTC_Bcd2ToByte(RTC_AlarmStruct->RTC_AlarmDateWeekDay);
      assert_param(IS_RTC_ALARM_DATE_WEEKDAY_WEEKDAY(tmpreg));      
    }    
  }

  /* Check the input parameters format */
  if (RTC_Format != RTC_Format_BIN)
  {
    tmpreg = (((uint32_t)(RTC_AlarmStruct->RTC_AlarmTime.RTC_Hours) << 16) | \
              ((uint32_t)(RTC_AlarmStruct->RTC_AlarmTime.RTC_Minutes) << 8) | \
              ((uint32_t)RTC_AlarmStruct->RTC_AlarmTime.RTC_Seconds) | \
              ((uint32_t)(RTC_AlarmStruct->RTC_AlarmTime.RTC_H12) << 16) | \
              ((uint32_t)(RTC_AlarmStruct->RTC_AlarmDateWeekDay) << 24) | \
              ((uint32_t)RTC_AlarmStruct->RTC_AlarmDateWeekDaySel) | \
              ((uint32_t)RTC_AlarmStruct->RTC_AlarmMask)); 
  }  
  else
  {
    tmpreg = (((uint32_t)RTC_ByteToBcd2(RTC_AlarmStruct->RTC_AlarmTime.RTC_Hours) << 16) | \
              ((uint32_t)RTC_ByteToBcd2(RTC_AlarmStruct->RTC_AlarmTime.RTC_Minutes) << 8) | \
              ((uint32_t)RTC_ByteToBcd2(RTC_AlarmStruct->RTC_AlarmTime.RTC_Seconds)) | \
              ((uint32_t)(RTC_AlarmStruct->RTC_AlarmTime.RTC_H12) << 16) | \
              ((uint32_t)RTC_ByteToBcd2(RTC_AlarmStruct->RTC_AlarmDateWeekDay) << 24) | \
              ((uint32_t)RTC_AlarmStruct->RTC_AlarmDateWeekDaySel) | \
              ((uint32_t)RTC_AlarmStruct->RTC_AlarmMask)); 
  } 

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;

  /* Configure the Alarm register */
  if (RTC_Alarm == RTC_Alarm_A)
  {
    RTC->ALRMAR = (uint32_t)tmpreg;
  }
  else
  {
    RTC->ALRMBR = (uint32_t)tmpreg;
  }

  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF;   
}

/**
  * @brief  Fills each RTC_AlarmStruct member with its default value
  *         (Time = 00h:00mn:00sec / Date = 1st day of the month/Mask =
  *         all fields are masked).
  * @param  RTC_AlarmStruct: pointer to a @ref RTC_AlarmTypeDef structure which
  *         will be initialized.
  * @retval None
  */
void RTC_AlarmStructInit(RTC_AlarmTypeDef* RTC_AlarmStruct)
{
  /* Alarm Time Settings : Time = 00h:00mn:00sec */
  RTC_AlarmStruct->RTC_AlarmTime.RTC_H12 = RTC_H12_AM;
  RTC_AlarmStruct->RTC_AlarmTime.RTC_Hours = 0;
  RTC_AlarmStruct->RTC_AlarmTime.RTC_Minutes = 0;
  RTC_AlarmStruct->RTC_AlarmTime.RTC_Seconds = 0;

  /* Alarm Date Settings : Date = 1st day of the month */
  RTC_AlarmStruct->RTC_AlarmDateWeekDaySel = RTC_AlarmDateWeekDaySel_Date;
  RTC_AlarmStruct->RTC_AlarmDateWeekDay = 1;

  /* Alarm Masks Settings : Mask =  all fields are not masked */
  RTC_AlarmStruct->RTC_AlarmMask = RTC_AlarmMask_None;
}

/**
  * @brief  Get the RTC Alarm value and masks.
  * @param  RTC_Format: specifies the format of the output parameters.
  *          This parameter can be one of the following values:
  *            @arg RTC_Format_BIN: Binary data format 
  *            @arg RTC_Format_BCD: BCD data format
  * @param  RTC_Alarm: specifies the alarm to be read.
  *          This parameter can be one of the following values:
  *            @arg RTC_Alarm_A: to select Alarm A
  *            @arg RTC_Alarm_B: to select Alarm B  
  * @param  RTC_AlarmStruct: pointer to a RTC_AlarmTypeDef structure that will 
  *                          contains the output alarm configuration values.     
  * @retval None
  */
void RTC_GetAlarm(uint32_t RTC_Format, uint32_t RTC_Alarm, RTC_AlarmTypeDef* RTC_AlarmStruct)
{
  uint32_t tmpreg = 0;

  /* Check the parameters */
  assert_param(IS_RTC_FORMAT(RTC_Format));
  assert_param(IS_RTC_ALARM(RTC_Alarm)); 

  /* Get the RTC_ALRMxR register */
  if (RTC_Alarm == RTC_Alarm_A)
  {
    tmpreg = (uint32_t)(RTC->ALRMAR);
  }
  else
  {
    tmpreg = (uint32_t)(RTC->ALRMBR);
  }

  /* Fill the structure with the read parameters */
  RTC_AlarmStruct->RTC_AlarmTime.RTC_Hours = (uint32_t)((tmpreg & (RTC_ALRMAR_HT | \
                                                     RTC_ALRMAR_HU)) >> 16);
  RTC_AlarmStruct->RTC_AlarmTime.RTC_Minutes = (uint32_t)((tmpreg & (RTC_ALRMAR_MNT | \
                                                     RTC_ALRMAR_MNU)) >> 8);
  RTC_AlarmStruct->RTC_AlarmTime.RTC_Seconds = (uint32_t)(tmpreg & (RTC_ALRMAR_ST | \
                                                     RTC_ALRMAR_SU));
  RTC_AlarmStruct->RTC_AlarmTime.RTC_H12 = (uint32_t)((tmpreg & RTC_ALRMAR_PM) >> 16);
  RTC_AlarmStruct->RTC_AlarmDateWeekDay = (uint32_t)((tmpreg & (RTC_ALRMAR_DT | RTC_ALRMAR_DU)) >> 24);
  RTC_AlarmStruct->RTC_AlarmDateWeekDaySel = (uint32_t)(tmpreg & RTC_ALRMAR_WDSEL);
  RTC_AlarmStruct->RTC_AlarmMask = (uint32_t)(tmpreg & RTC_AlarmMask_All);

  if (RTC_Format == RTC_Format_BIN)
  {
    RTC_AlarmStruct->RTC_AlarmTime.RTC_Hours = RTC_Bcd2ToByte(RTC_AlarmStruct-> \
                                                        RTC_AlarmTime.RTC_Hours);
    RTC_AlarmStruct->RTC_AlarmTime.RTC_Minutes = RTC_Bcd2ToByte(RTC_AlarmStruct-> \
                                                        RTC_AlarmTime.RTC_Minutes);
    RTC_AlarmStruct->RTC_AlarmTime.RTC_Seconds = RTC_Bcd2ToByte(RTC_AlarmStruct-> \
                                                        RTC_AlarmTime.RTC_Seconds);
    RTC_AlarmStruct->RTC_AlarmDateWeekDay = RTC_Bcd2ToByte(RTC_AlarmStruct->RTC_AlarmDateWeekDay);
  }  
}

/**
  * @brief  Enables or disables the specified RTC Alarm.
  * @param  RTC_Alarm: specifies the alarm to be configured.
  *          This parameter can be any combination of the following values:
  *            @arg RTC_Alarm_A: to select Alarm A
  *            @arg RTC_Alarm_B: to select Alarm B  
  * @param  NewState: new state of the specified alarm.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: RTC Alarm is enabled/disabled
  *          - ERROR: RTC Alarm is not enabled/disabled  
  */
ErrorStatus RTC_AlarmCmd(uint32_t RTC_Alarm, FunctionalState NewState)
{
  __IO uint32_t alarmcounter = 0x00;
  uint32_t alarmstatus = 0x00;
  ErrorStatus status = ERROR;
    
  /* Check the parameters */
  assert_param(IS_RTC_CMD_ALARM(RTC_Alarm));
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;

  /* Configure the Alarm state */
  if (NewState != DISABLE)
  {
    RTC->CR |= (uint32_t)RTC_Alarm;

    status = SUCCESS;    
  }
  else
  { 
    /* Disable the Alarm in RTC_CR register */
    RTC->CR &= (uint32_t)~RTC_Alarm;
   
    /* Wait till RTC ALRxWF flag is set and if Time out is reached exit */
    do
    {
      alarmstatus = RTC->ISR & (RTC_Alarm >> 8);
      alarmcounter++;  
    } while((alarmcounter != INITMODE_TIMEOUT) && (alarmstatus == 0x00));
    
    if ((RTC->ISR & (RTC_Alarm >> 8)) == RESET)
    {
      status = ERROR;
    } 
    else
    {
      status = SUCCESS;
    }        
  } 

  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF; 
  
  return status;
}

/**
  * @brief  Configure the RTC AlarmA/B Subseconds value and mask.*
  * @note   This function is performed only when the Alarm is disabled. 
  * @param  RTC_Alarm: specifies the alarm to be configured.
  *   This parameter can be one of the following values:
  *     @arg RTC_Alarm_A: to select Alarm A
  *     @arg RTC_Alarm_B: to select Alarm B
  * @param  RTC_AlarmSubSecondValue: specifies the Subseconds value.
  *   This parameter can be a value from 0 to 0x00007FFF.
  * @param  RTC_AlarmSubSecondMask:  specifies the Subseconds Mask.
  *   This parameter can be any combination of the following values:
  *     @arg RTC_AlarmSubSecondMask_All    : All Alarm SS fields are masked.
  *                                          There is no comparison on sub seconds for Alarm.
  *     @arg RTC_AlarmSubSecondMask_SS14_1 : SS[14:1] are don't care in Alarm comparison.
  *                                          Only SS[0] is compared
  *     @arg RTC_AlarmSubSecondMask_SS14_2 : SS[14:2] are don't care in Alarm comparison.
  *                                          Only SS[1:0] are compared
  *     @arg RTC_AlarmSubSecondMask_SS14_3 : SS[14:3] are don't care in Alarm comparison.
  *                                          Only SS[2:0] are compared
  *     @arg RTC_AlarmSubSecondMask_SS14_4 : SS[14:4] are don't care in Alarm comparison.
  *                                          Only SS[3:0] are compared
  *     @arg RTC_AlarmSubSecondMask_SS14_5 : SS[14:5] are don't care in Alarm comparison.
  *                                          Only SS[4:0] are compared
  *     @arg RTC_AlarmSubSecondMask_SS14_6 : SS[14:6] are don't care in Alarm comparison.
  *                                          Only SS[5:0] are compared
  *     @arg RTC_AlarmSubSecondMask_SS14_7 : SS[14:7] are don't care in Alarm comparison.
  *                                          Only SS[6:0] are compared
  *     @arg RTC_AlarmSubSecondMask_SS14_8 : SS[14:8] are don't care in Alarm comparison.
  *                                          Only SS[7:0] are compared
  *     @arg RTC_AlarmSubSecondMask_SS14_9 : SS[14:9] are don't care in Alarm comparison.
  *                                          Only SS[8:0] are compared
  *     @arg RTC_AlarmSubSecondMask_SS14_10: SS[14:10] are don't care in Alarm comparison.
  *                                          Only SS[9:0] are compared
  *     @arg RTC_AlarmSubSecondMask_SS14_11: SS[14:11] are don't care in Alarm comparison.
  *                                          Only SS[10:0] are compared
  *     @arg RTC_AlarmSubSecondMask_SS14_12: SS[14:12] are don't care in Alarm comparison.
  *                                          Only SS[11:0] are compared
  *     @arg RTC_AlarmSubSecondMask_SS14_13: SS[14:13] are don't care in Alarm comparison.
  *                                          Only SS[12:0] are compared
  *     @arg RTC_AlarmSubSecondMask_SS14   : SS[14] is don't care in Alarm comparison.
  *                                          Only SS[13:0] are compared
  *     @arg RTC_AlarmSubSecondMask_None   : SS[14:0] are compared and must match
  *                                          to activate alarm
  * @retval None
  */
void RTC_AlarmSubSecondConfig(uint32_t RTC_Alarm, uint32_t RTC_AlarmSubSecondValue, uint32_t RTC_AlarmSubSecondMask)
{
  uint32_t tmpreg = 0;

  /* Check the parameters */
  assert_param(IS_RTC_ALARM(RTC_Alarm));
  assert_param(IS_RTC_ALARM_SUB_SECOND_VALUE(RTC_AlarmSubSecondValue));
  assert_param(IS_RTC_ALARM_SUB_SECOND_MASK(RTC_AlarmSubSecondMask));
  
  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;
  
  /* Configure the Alarm A or Alarm B SubSecond registers */
  tmpreg = (uint32_t) (uint32_t)(RTC_AlarmSubSecondValue) | (uint32_t)(RTC_AlarmSubSecondMask);
  
  if (RTC_Alarm == RTC_Alarm_A)
  {
    /* Configure the AlarmA SubSecond register */
    RTC->ALRMASSR = tmpreg;
  }
  else
  {
    /* Configure the Alarm B SubSecond register */
    RTC->ALRMBSSR = tmpreg;
  }

  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF;

}

/**
  * @brief  Gets the RTC Alarm Subseconds value.
  * @param  RTC_Alarm: specifies the alarm to be read.
  *   This parameter can be one of the following values:
  *     @arg RTC_Alarm_A: to select Alarm A
  *     @arg RTC_Alarm_B: to select Alarm B
  * @param  None
  * @retval RTC Alarm Subseconds value.
  */
uint32_t RTC_GetAlarmSubSecond(uint32_t RTC_Alarm)
{
  uint32_t tmpreg = 0;
  
  /* Get the RTC_ALRMxR register */
  if (RTC_Alarm == RTC_Alarm_A)
  {
    tmpreg = (uint32_t)((RTC->ALRMASSR) & RTC_ALRMASSR_SS);
  }
  else
  {
    tmpreg = (uint32_t)((RTC->ALRMBSSR) & RTC_ALRMBSSR_SS);
  } 
  
  return (tmpreg);
}

/**
  * @}
  */

/** @defgroup RTC_Group4 WakeUp Timer configuration functions
 *  @brief   WakeUp Timer configuration functions 
 *
@verbatim   
 ===============================================================================
                     WakeUp Timer configuration functions
 ===============================================================================  

  This section provide functions allowing to program and read the RTC WakeUp.

@endverbatim
  * @{
  */

/**
  * @brief  Configures the RTC Wakeup clock source.
  * @note   The WakeUp Clock source can only be changed when the RTC WakeUp
  *         is disabled (Use the RTC_WakeUpCmd(DISABLE)).      
  * @param  RTC_WakeUpClock: Wakeup Clock source.
  *          This parameter can be one of the following values:
  *            @arg RTC_WakeUpClock_RTCCLK_Div16: RTC Wakeup Counter Clock = RTCCLK/16
  *            @arg RTC_WakeUpClock_RTCCLK_Div8: RTC Wakeup Counter Clock = RTCCLK/8
  *            @arg RTC_WakeUpClock_RTCCLK_Div4: RTC Wakeup Counter Clock = RTCCLK/4
  *            @arg RTC_WakeUpClock_RTCCLK_Div2: RTC Wakeup Counter Clock = RTCCLK/2
  *            @arg RTC_WakeUpClock_CK_SPRE_16bits: RTC Wakeup Counter Clock = CK_SPRE
  *            @arg RTC_WakeUpClock_CK_SPRE_17bits: RTC Wakeup Counter Clock = CK_SPRE
  * @retval None
  */
void RTC_WakeUpClockConfig(uint32_t RTC_WakeUpClock)
{
  /* Check the parameters */
  assert_param(IS_RTC_WAKEUP_CLOCK(RTC_WakeUpClock));

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;

  /* Clear the Wakeup Timer clock source bits in CR register */
  RTC->CR &= (uint32_t)~RTC_CR_WUCKSEL;

  /* Configure the clock source */
  RTC->CR |= (uint32_t)RTC_WakeUpClock;
  
  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF; 
}

/**
  * @brief  Configures the RTC Wakeup counter.
  * @note   The RTC WakeUp counter can only be written when the RTC WakeUp
  *         is disabled (Use the RTC_WakeUpCmd(DISABLE)).        
  * @param  RTC_WakeUpCounter: specifies the WakeUp counter.
  *          This parameter can be a value from 0x0000 to 0xFFFF. 
  * @retval None
  */
void RTC_SetWakeUpCounter(uint32_t RTC_WakeUpCounter)
{
  /* Check the parameters */
  assert_param(IS_RTC_WAKEUP_COUNTER(RTC_WakeUpCounter));
  
  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;
  
  /* Configure the Wakeup Timer counter */
  RTC->WUTR = (uint32_t)RTC_WakeUpCounter;
  
  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF; 
}

/**
  * @brief  Returns the RTC WakeUp timer counter value.
  * @param  None
  * @retval The RTC WakeUp Counter value.
  */
uint32_t RTC_GetWakeUpCounter(void)
{
  /* Get the counter value */
  return ((uint32_t)(RTC->WUTR & RTC_WUTR_WUT));
}

/**
  * @brief  Enables or Disables the RTC WakeUp timer.
  * @param  NewState: new state of the WakeUp timer.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
ErrorStatus RTC_WakeUpCmd(FunctionalState NewState)
{
  __IO uint32_t wutcounter = 0x00;
  uint32_t wutwfstatus = 0x00;
  ErrorStatus status = ERROR;
  
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;

  if (NewState != DISABLE)
  {
    /* Enable the Wakeup Timer */
    RTC->CR |= (uint32_t)RTC_CR_WUTE;
    status = SUCCESS;    
  }
  else
  {
    /* Disable the Wakeup Timer */
    RTC->CR &= (uint32_t)~RTC_CR_WUTE;
    /* Wait till RTC WUTWF flag is set and if Time out is reached exit */
    do
    {
      wutwfstatus = RTC->ISR & RTC_ISR_WUTWF;
      wutcounter++;  
    } while((wutcounter != INITMODE_TIMEOUT) && (wutwfstatus == 0x00));
    
    if ((RTC->ISR & RTC_ISR_WUTWF) == RESET)
    {
      status = ERROR;
    }
    else
    {
      status = SUCCESS;
    }    
  }

  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF; 
  
  return status;
}

/**
  * @}
  */

/** @defgroup RTC_Group5 Daylight Saving configuration functions
 *  @brief   Daylight Saving configuration functions 
 *
@verbatim   
 ===============================================================================
                    Daylight Saving configuration functions
 ===============================================================================  

  This section provide functions allowing to configure the RTC DayLight Saving.

@endverbatim
  * @{
  */

/**
  * @brief  Adds or substract one hour from the current time.
  * @param  RTC_DayLightSaveOperation: the value of hour adjustment. 
  *          This parameter can be one of the following values:
  *            @arg RTC_DayLightSaving_SUB1H: Substract one hour (winter time)
  *            @arg RTC_DayLightSaving_ADD1H: Add one hour (summer time)
  * @param  RTC_StoreOperation: Specifies the value to be written in the BCK bit 
  *                            in CR register to store the operation.
  *          This parameter can be one of the following values:
  *            @arg RTC_StoreOperation_Reset: BCK Bit Reset
  *            @arg RTC_StoreOperation_Set: BCK Bit Set
  * @retval None
  */
void RTC_DayLightSavingConfig(uint32_t RTC_DayLightSaving, uint32_t RTC_StoreOperation)
{
  /* Check the parameters */
  assert_param(IS_RTC_DAYLIGHT_SAVING(RTC_DayLightSaving));
  assert_param(IS_RTC_STORE_OPERATION(RTC_StoreOperation));

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;

  /* Clear the bits to be configured */
  RTC->CR &= (uint32_t)~(RTC_CR_BCK);

  /* Configure the RTC_CR register */
  RTC->CR |= (uint32_t)(RTC_DayLightSaving | RTC_StoreOperation);

  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF; 
}

/**
  * @brief  Returns the RTC Day Light Saving stored operation.
  * @param  None
  * @retval RTC Day Light Saving stored operation.
  *          - RTC_StoreOperation_Reset
  *          - RTC_StoreOperation_Set       
  */
uint32_t RTC_GetStoreOperation(void)
{
  return (RTC->CR & RTC_CR_BCK);
}

/**
  * @}
  */

/** @defgroup RTC_Group6 Output pin Configuration function
 *  @brief   Output pin Configuration function 
 *
@verbatim   
 ===============================================================================
                         Output pin Configuration function
 ===============================================================================  

  This section provide functions allowing to configure the RTC Output source.

@endverbatim
  * @{
  */

/**
  * @brief  Configures the RTC output source (AFO_ALARM).
  * @param  RTC_Output: Specifies which signal will be routed to the RTC output. 
  *          This parameter can be one of the following values:
  *            @arg RTC_Output_Disable: No output selected
  *            @arg RTC_Output_AlarmA: signal of AlarmA mapped to output
  *            @arg RTC_Output_AlarmB: signal of AlarmB mapped to output
  *            @arg RTC_Output_WakeUp: signal of WakeUp mapped to output
  * @param  RTC_OutputPolarity: Specifies the polarity of the output signal. 
  *          This parameter can be one of the following:
  *            @arg RTC_OutputPolarity_High: The output pin is high when the 
  *                                 ALRAF/ALRBF/WUTF is high (depending on OSEL)
  *            @arg RTC_OutputPolarity_Low: The output pin is low when the 
  *                                 ALRAF/ALRBF/WUTF is high (depending on OSEL)
  * @retval None
  */
void RTC_OutputConfig(uint32_t RTC_Output, uint32_t RTC_OutputPolarity)
{
  /* Check the parameters */
  assert_param(IS_RTC_OUTPUT(RTC_Output));
  assert_param(IS_RTC_OUTPUT_POL(RTC_OutputPolarity));

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;

  /* Clear the bits to be configured */
  RTC->CR &= (uint32_t)~(RTC_CR_OSEL | RTC_CR_POL);

  /* Configure the output selection and polarity */
  RTC->CR |= (uint32_t)(RTC_Output | RTC_OutputPolarity);

  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF; 
}

/**
  * @}
  */

/** @defgroup RTC_Group7 Digital Calibration configuration functions
 *  @brief   Coarse Calibration configuration functions 
 *
@verbatim   
 ===============================================================================
                  Digital Calibration configuration functions
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Configures the Coarse calibration parameters.
  * @param  RTC_CalibSign: specifies the sign of the coarse calibration value.
  *          This parameter can be  one of the following values:
  *            @arg RTC_CalibSign_Positive: The value sign is positive 
  *            @arg RTC_CalibSign_Negative: The value sign is negative
  * @param  Value: value of coarse calibration expressed in ppm (coded on 5 bits).
  *    
  * @note   This Calibration value should be between 0 and 63 when using negative
  *         sign with a 2-ppm step.
  *           
  * @note   This Calibration value should be between 0 and 126 when using positive
  *         sign with a 4-ppm step.
  *           
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: RTC Coarse calibration are initialized
  *          - ERROR: RTC Coarse calibration are not initialized     
  */
ErrorStatus RTC_CoarseCalibConfig(uint32_t RTC_CalibSign, uint32_t Value)
{
  ErrorStatus status = ERROR;
   
  /* Check the parameters */
  assert_param(IS_RTC_CALIB_SIGN(RTC_CalibSign));
  assert_param(IS_RTC_CALIB_VALUE(Value)); 

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;

  /* Set Initialization mode */
  if (RTC_EnterInitMode() == ERROR)
  {
    status = ERROR;
  } 
  else
  {
    /* Set the coarse calibration value */
    RTC->CALIBR = (uint32_t)(RTC_CalibSign | Value);
    /* Exit Initialization mode */
    RTC_ExitInitMode();
    
    status = SUCCESS;
  } 

  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF; 
  
  return status;
}

/**
  * @brief  Enables or disables the Coarse calibration process.
  * @param  NewState: new state of the Coarse calibration.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: RTC Coarse calibration are enabled/disabled
  *          - ERROR: RTC Coarse calibration are not enabled/disabled    
  */
ErrorStatus RTC_CoarseCalibCmd(FunctionalState NewState)
{
  ErrorStatus status = ERROR;
  
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;
  
  /* Set Initialization mode */
  if (RTC_EnterInitMode() == ERROR)
  {
    status =  ERROR;
  }
  else
  {
    if (NewState != DISABLE)
    {
      /* Enable the Coarse Calibration */
      RTC->CR |= (uint32_t)RTC_CR_DCE;
    }
    else
    { 
      /* Disable the Coarse Calibration */
      RTC->CR &= (uint32_t)~RTC_CR_DCE;
    }
    /* Exit Initialization mode */
    RTC_ExitInitMode();
    
    status = SUCCESS;
  } 
  
  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF; 
  
  return status;
}

/**
  * @brief  Enables or disables the RTC clock to be output through the relative pin.
  * @param  NewState: new state of the digital calibration Output.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void RTC_CalibOutputCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;
  
  if (NewState != DISABLE)
  {
    /* Enable the RTC clock output */
    RTC->CR |= (uint32_t)RTC_CR_COE;
  }
  else
  { 
    /* Disable the RTC clock output */
    RTC->CR &= (uint32_t)~RTC_CR_COE;
  }
  
  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF; 
}

/**
  * @brief  Configure the Calibration Pinout (RTC_CALIB) Selection (1Hz or 512Hz).
  * @param  RTC_CalibOutput : Select the Calibration output Selection .
  *   This parameter can be one of the following values:
  *     @arg RTC_CalibOutput_512Hz: A signal has a regular waveform at 512Hz. 
  *     @arg RTC_CalibOutput_1Hz  : A signal has a regular waveform at 1Hz.
  * @retval None
*/
void RTC_CalibOutputConfig(uint32_t RTC_CalibOutput)
{
  /* Check the parameters */
  assert_param(IS_RTC_CALIB_OUTPUT(RTC_CalibOutput));

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;
  
  /*clear flags before config*/
  RTC->CR &= (uint32_t)~(RTC_CR_COSEL);

  /* Configure the RTC_CR register */
  RTC->CR |= (uint32_t)RTC_CalibOutput;

  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF;
}

/**
  * @brief  Configures the Smooth Calibration Settings.
  * @param  RTC_SmoothCalibPeriod : Select the Smooth Calibration Period.
  *   This parameter can be can be one of the following values:
  *     @arg RTC_SmoothCalibPeriod_32sec : The smooth calibration periode is 32s.
  *     @arg RTC_SmoothCalibPeriod_16sec : The smooth calibration periode is 16s.
  *     @arg RTC_SmoothCalibPeriod_8sec  : The smooth calibartion periode is 8s.
  * @param  RTC_SmoothCalibPlusPulses : Select to Set or reset the CALP bit.
  *   This parameter can be one of the following values:
  *     @arg RTC_SmoothCalibPlusPulses_Set  : Add one RTCCLK puls every 2**11 pulses.
  *     @arg RTC_SmoothCalibPlusPulses_Reset: No RTCCLK pulses are added.
  * @param  RTC_SmouthCalibMinusPulsesValue: Select the value of CALM[8:0] bits.
  *   This parameter can be one any value from 0 to 0x000001FF.
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: RTC Calib registers are configured
  *          - ERROR: RTC Calib registers are not configured
*/
ErrorStatus RTC_SmoothCalibConfig(uint32_t RTC_SmoothCalibPeriod,
                                  uint32_t RTC_SmoothCalibPlusPulses,
                                  uint32_t RTC_SmouthCalibMinusPulsesValue)
{
  ErrorStatus status = ERROR;
  uint32_t recalpfcount = 0;

  /* Check the parameters */
  assert_param(IS_RTC_SMOOTH_CALIB_PERIOD(RTC_SmoothCalibPeriod));
  assert_param(IS_RTC_SMOOTH_CALIB_PLUS(RTC_SmoothCalibPlusPulses));
  assert_param(IS_RTC_SMOOTH_CALIB_MINUS(RTC_SmouthCalibMinusPulsesValue));

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;
  
  /* check if a calibration is pending*/
  if ((RTC->ISR & RTC_ISR_RECALPF) != RESET)
  {
    /* wait until the Calibration is completed*/
    while (((RTC->ISR & RTC_ISR_RECALPF) != RESET) && (recalpfcount != RECALPF_TIMEOUT))
    {
      recalpfcount++;
    }
  }

  /* check if the calibration pending is completed or if there is no calibration operation at all*/
  if ((RTC->ISR & RTC_ISR_RECALPF) == RESET)
  {
    /* Configure the Smooth calibration settings */
    RTC->CALR = (uint32_t)((uint32_t)RTC_SmoothCalibPeriod | (uint32_t)RTC_SmoothCalibPlusPulses | (uint32_t)RTC_SmouthCalibMinusPulsesValue);

    status = SUCCESS;
  }
  else
  {
    status = ERROR;
  }

  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF;
  
  return (ErrorStatus)(status);
}

/**
  * @}
  */


/** @defgroup RTC_Group8 TimeStamp configuration functions
 *  @brief   TimeStamp configuration functions 
 *
@verbatim   
 ===============================================================================
                       TimeStamp configuration functions
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Enables or Disables the RTC TimeStamp functionality with the 
  *         specified time stamp pin stimulating edge.
  * @param  RTC_TimeStampEdge: Specifies the pin edge on which the TimeStamp is 
  *         activated.
  *          This parameter can be one of the following:
  *            @arg RTC_TimeStampEdge_Rising: the Time stamp event occurs on the rising 
  *                                    edge of the related pin.
  *            @arg RTC_TimeStampEdge_Falling: the Time stamp event occurs on the 
  *                                     falling edge of the related pin.
  * @param  NewState: new state of the TimeStamp.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void RTC_TimeStampCmd(uint32_t RTC_TimeStampEdge, FunctionalState NewState)
{
  uint32_t tmpreg = 0;

  /* Check the parameters */
  assert_param(IS_RTC_TIMESTAMP_EDGE(RTC_TimeStampEdge));
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  /* Get the RTC_CR register and clear the bits to be configured */
  tmpreg = (uint32_t)(RTC->CR & (uint32_t)~(RTC_CR_TSEDGE | RTC_CR_TSE));

  /* Get the new configuration */
  if (NewState != DISABLE)
  {
    tmpreg |= (uint32_t)(RTC_TimeStampEdge | RTC_CR_TSE);
  }
  else
  {
    tmpreg |= (uint32_t)(RTC_TimeStampEdge);
  }

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;

  /* Configure the Time Stamp TSEDGE and Enable bits */
  RTC->CR = (uint32_t)tmpreg;

  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF; 
}

/**
  * @brief  Get the RTC TimeStamp value and masks.
  * @param  RTC_Format: specifies the format of the output parameters.
  *          This parameter can be one of the following values:
  *            @arg RTC_Format_BIN: Binary data format 
  *            @arg RTC_Format_BCD: BCD data format
  * @param RTC_StampTimeStruct: pointer to a RTC_TimeTypeDef structure that will 
  *                             contains the TimeStamp time values. 
  * @param RTC_StampDateStruct: pointer to a RTC_DateTypeDef structure that will 
  *                             contains the TimeStamp date values.     
  * @retval None
  */
void RTC_GetTimeStamp(uint32_t RTC_Format, RTC_TimeTypeDef* RTC_StampTimeStruct, 
                                      RTC_DateTypeDef* RTC_StampDateStruct)
{
  uint32_t tmptime = 0, tmpdate = 0;

  /* Check the parameters */
  assert_param(IS_RTC_FORMAT(RTC_Format));

  /* Get the TimeStamp time and date registers values */
  tmptime = (uint32_t)(RTC->TSTR & RTC_TR_RESERVED_MASK);
  tmpdate = (uint32_t)(RTC->TSDR & RTC_DR_RESERVED_MASK);

  /* Fill the Time structure fields with the read parameters */
  RTC_StampTimeStruct->RTC_Hours = (uint8_t)((tmptime & (RTC_TR_HT | RTC_TR_HU)) >> 16);
  RTC_StampTimeStruct->RTC_Minutes = (uint8_t)((tmptime & (RTC_TR_MNT | RTC_TR_MNU)) >> 8);
  RTC_StampTimeStruct->RTC_Seconds = (uint8_t)(tmptime & (RTC_TR_ST | RTC_TR_SU));
  RTC_StampTimeStruct->RTC_H12 = (uint8_t)((tmptime & (RTC_TR_PM)) >> 16);  

  /* Fill the Date structure fields with the read parameters */
  RTC_StampDateStruct->RTC_Year = 0;
  RTC_StampDateStruct->RTC_Month = (uint8_t)((tmpdate & (RTC_DR_MT | RTC_DR_MU)) >> 8);
  RTC_StampDateStruct->RTC_Date = (uint8_t)(tmpdate & (RTC_DR_DT | RTC_DR_DU));
  RTC_StampDateStruct->RTC_WeekDay = (uint8_t)((tmpdate & (RTC_DR_WDU)) >> 13);

  /* Check the input parameters format */
  if (RTC_Format == RTC_Format_BIN)
  {
    /* Convert the Time structure parameters to Binary format */
    RTC_StampTimeStruct->RTC_Hours = (uint8_t)RTC_Bcd2ToByte(RTC_StampTimeStruct->RTC_Hours);
    RTC_StampTimeStruct->RTC_Minutes = (uint8_t)RTC_Bcd2ToByte(RTC_StampTimeStruct->RTC_Minutes);
    RTC_StampTimeStruct->RTC_Seconds = (uint8_t)RTC_Bcd2ToByte(RTC_StampTimeStruct->RTC_Seconds);

    /* Convert the Date structure parameters to Binary format */
    RTC_StampDateStruct->RTC_Month = (uint8_t)RTC_Bcd2ToByte(RTC_StampDateStruct->RTC_Month);
    RTC_StampDateStruct->RTC_Date = (uint8_t)RTC_Bcd2ToByte(RTC_StampDateStruct->RTC_Date);
    RTC_StampDateStruct->RTC_WeekDay = (uint8_t)RTC_Bcd2ToByte(RTC_StampDateStruct->RTC_WeekDay);
  }
}

/**
  * @brief  Get the RTC timestamp Subseconds value.
  * @param  None
  * @retval RTC current timestamp Subseconds value.
  */
uint32_t RTC_GetTimeStampSubSecond(void)
{
  /* Get timestamp subseconds values from the correspondent registers */
  return (uint32_t)(RTC->TSSSR);
}

/**
  * @}
  */

/** @defgroup RTC_Group9 Tampers configuration functions
 *  @brief   Tampers configuration functions 
 *
@verbatim   
 ===============================================================================
                       Tampers configuration functions
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Configures the select Tamper pin edge.
  * @param  RTC_Tamper: Selected tamper pin.
  *          This parameter can be RTC_Tamper_1.
  * @param  RTC_TamperTrigger: Specifies the trigger on the tamper pin that 
  *         stimulates tamper event. 
  *   This parameter can be one of the following values:
  *     @arg RTC_TamperTrigger_RisingEdge: Rising Edge of the tamper pin causes tamper event.
  *     @arg RTC_TamperTrigger_FallingEdge: Falling Edge of the tamper pin causes tamper event.
  *     @arg RTC_TamperTrigger_LowLevel: Low Level of the tamper pin causes tamper event.
  *     @arg RTC_TamperTrigger_HighLevel: High Level of the tamper pin causes tamper event.
  * @retval None
  */
void RTC_TamperTriggerConfig(uint32_t RTC_Tamper, uint32_t RTC_TamperTrigger)
{
  /* Check the parameters */
  assert_param(IS_RTC_TAMPER(RTC_Tamper)); 
  assert_param(IS_RTC_TAMPER_TRIGGER(RTC_TamperTrigger));
 
  if (RTC_TamperTrigger == RTC_TamperTrigger_RisingEdge)
  {  
    /* Configure the RTC_TAFCR register */
    RTC->TAFCR &= (uint32_t)((uint32_t)~(RTC_Tamper << 1));	
  }
  else
  { 
    /* Configure the RTC_TAFCR register */
    RTC->TAFCR |= (uint32_t)(RTC_Tamper << 1);  
  }  
}

/**
  * @brief  Enables or Disables the Tamper detection.
  * @param  RTC_Tamper: Selected tamper pin.
  *          This parameter can be RTC_Tamper_1.
  * @param  NewState: new state of the tamper pin.
  *          This parameter can be: ENABLE or DISABLE.                   
  * @retval None
  */
void RTC_TamperCmd(uint32_t RTC_Tamper, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_RTC_TAMPER(RTC_Tamper));  
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if (NewState != DISABLE)
  {
    /* Enable the selected Tamper pin */
    RTC->TAFCR |= (uint32_t)RTC_Tamper;
  }
  else
  {
    /* Disable the selected Tamper pin */
    RTC->TAFCR &= (uint32_t)~RTC_Tamper;    
  }  
}

/**
  * @brief  Configures the Tampers Filter.
  * @param  RTC_TamperFilter: Specifies the tampers filter.
  *   This parameter can be one of the following values:
  *     @arg RTC_TamperFilter_Disable: Tamper filter is disabled.
  *     @arg RTC_TamperFilter_2Sample: Tamper is activated after 2 consecutive 
  *                                    samples at the active level 
  *     @arg RTC_TamperFilter_4Sample: Tamper is activated after 4 consecutive 
  *                                    samples at the active level
  *     @arg RTC_TamperFilter_8Sample: Tamper is activated after 8 consecutive 
  *                                    samples at the active level 
  * @retval None
  */
void RTC_TamperFilterConfig(uint32_t RTC_TamperFilter)
{
  /* Check the parameters */
  assert_param(IS_RTC_TAMPER_FILTER(RTC_TamperFilter));
   
  /* Clear TAMPFLT[1:0] bits in the RTC_TAFCR register */
  RTC->TAFCR &= (uint32_t)~(RTC_TAFCR_TAMPFLT);

  /* Configure the RTC_TAFCR register */
  RTC->TAFCR |= (uint32_t)RTC_TamperFilter;
}

/**
  * @brief  Configures the Tampers Sampling Frequency.
  * @param  RTC_TamperSamplingFreq: Specifies the tampers Sampling Frequency.
  *   This parameter can be one of the following values:
  *     @arg RTC_TamperSamplingFreq_RTCCLK_Div32768: Each of the tamper inputs are sampled
  *                                           with a frequency =  RTCCLK / 32768
  *     @arg RTC_TamperSamplingFreq_RTCCLK_Div16384: Each of the tamper inputs are sampled
  *                                           with a frequency =  RTCCLK / 16384
  *     @arg RTC_TamperSamplingFreq_RTCCLK_Div8192: Each of the tamper inputs are sampled
  *                                           with a frequency =  RTCCLK / 8192
  *     @arg RTC_TamperSamplingFreq_RTCCLK_Div4096: Each of the tamper inputs are sampled
  *                                           with a frequency =  RTCCLK / 4096
  *     @arg RTC_TamperSamplingFreq_RTCCLK_Div2048: Each of the tamper inputs are sampled
  *                                           with a frequency =  RTCCLK / 2048
  *     @arg RTC_TamperSamplingFreq_RTCCLK_Div1024: Each of the tamper inputs are sampled
  *                                           with a frequency =  RTCCLK / 1024
  *     @arg RTC_TamperSamplingFreq_RTCCLK_Div512: Each of the tamper inputs are sampled
  *                                           with a frequency =  RTCCLK / 512  
  *     @arg RTC_TamperSamplingFreq_RTCCLK_Div256: Each of the tamper inputs are sampled
  *                                           with a frequency =  RTCCLK / 256  
  * @retval None
  */
void RTC_TamperSamplingFreqConfig(uint32_t RTC_TamperSamplingFreq)
{
  /* Check the parameters */
  assert_param(IS_RTC_TAMPER_SAMPLING_FREQ(RTC_TamperSamplingFreq));
 
  /* Clear TAMPFREQ[2:0] bits in the RTC_TAFCR register */
  RTC->TAFCR &= (uint32_t)~(RTC_TAFCR_TAMPFREQ);

  /* Configure the RTC_TAFCR register */
  RTC->TAFCR |= (uint32_t)RTC_TamperSamplingFreq;
}

/**
  * @brief  Configures the Tampers Pins input Precharge Duration.
  * @param  RTC_TamperPrechargeDuration: Specifies the Tampers Pins input
  *         Precharge Duration.
  *   This parameter can be one of the following values:
  *     @arg RTC_TamperPrechargeDuration_1RTCCLK: Tamper pins are pre-charged before sampling during 1 RTCCLK cycle
  *     @arg RTC_TamperPrechargeDuration_2RTCCLK: Tamper pins are pre-charged before sampling during 2 RTCCLK cycle
  *     @arg RTC_TamperPrechargeDuration_4RTCCLK: Tamper pins are pre-charged before sampling during 4 RTCCLK cycle    
  *     @arg RTC_TamperPrechargeDuration_8RTCCLK: Tamper pins are pre-charged before sampling during 8 RTCCLK cycle
  * @retval None
  */
void RTC_TamperPinsPrechargeDuration(uint32_t RTC_TamperPrechargeDuration)
{
  /* Check the parameters */
  assert_param(IS_RTC_TAMPER_PRECHARGE_DURATION(RTC_TamperPrechargeDuration));
   
  /* Clear TAMPPRCH[1:0] bits in the RTC_TAFCR register */
  RTC->TAFCR &= (uint32_t)~(RTC_TAFCR_TAMPPRCH);

  /* Configure the RTC_TAFCR register */
  RTC->TAFCR |= (uint32_t)RTC_TamperPrechargeDuration;
}

/**
  * @brief  Enables or Disables the TimeStamp on Tamper Detection Event.
  * @note   The timestamp is valid even the TSE bit in tamper control register 
  *         is reset.   
  * @param  NewState: new state of the timestamp on tamper event.
  *         This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void RTC_TimeStampOnTamperDetectionCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
   
  if (NewState != DISABLE)
  {
    /* Save timestamp on tamper detection event */
    RTC->TAFCR |= (uint32_t)RTC_TAFCR_TAMPTS;
  }
  else
  {
    /* Tamper detection does not cause a timestamp to be saved */
    RTC->TAFCR &= (uint32_t)~RTC_TAFCR_TAMPTS;    
  }
}

/**
  * @brief  Enables or Disables the Precharge of Tamper pin.
  * @param  NewState: new state of tamper pull up.
  *   This parameter can be: ENABLE or DISABLE.                   
  * @retval None
  */
void RTC_TamperPullUpCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
 if (NewState != DISABLE)
  {
    /* Enable precharge of the selected Tamper pin */
    RTC->TAFCR &= (uint32_t)~RTC_TAFCR_TAMPPUDIS; 
  }
  else
  {
    /* Disable precharge of the selected Tamper pin */
    RTC->TAFCR |= (uint32_t)RTC_TAFCR_TAMPPUDIS;    
  } 
}

/**
  * @}
  */

/** @defgroup RTC_Group10 Backup Data Registers configuration functions
 *  @brief   Backup Data Registers configuration functions  
 *
@verbatim   
 ===============================================================================
                       Backup Data Registers configuration functions 
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Writes a data in a specified RTC Backup data register.
  * @param  RTC_BKP_DR: RTC Backup data Register number.
  *          This parameter can be: RTC_BKP_DRx where x can be from 0 to 19 to 
  *                          specify the register.
  * @param  Data: Data to be written in the specified RTC Backup data register.                     
  * @retval None
  */
void RTC_WriteBackupRegister(uint32_t RTC_BKP_DR, uint32_t Data)
{
  __IO uint32_t tmp = 0;
  
  /* Check the parameters */
  assert_param(IS_RTC_BKP(RTC_BKP_DR));

  tmp = RTC_BASE + 0x50;
  tmp += (RTC_BKP_DR * 4);

  /* Write the specified register */
  *(__IO uint32_t *)tmp = (uint32_t)Data;
}

/**
  * @brief  Reads data from the specified RTC Backup data Register.
  * @param  RTC_BKP_DR: RTC Backup data Register number.
  *          This parameter can be: RTC_BKP_DRx where x can be from 0 to 19 to 
  *                          specify the register.                   
  * @retval None
  */
uint32_t RTC_ReadBackupRegister(uint32_t RTC_BKP_DR)
{
  __IO uint32_t tmp = 0;
  
  /* Check the parameters */
  assert_param(IS_RTC_BKP(RTC_BKP_DR));

  tmp = RTC_BASE + 0x50;
  tmp += (RTC_BKP_DR * 4);
  
  /* Read the specified register */
  return (*(__IO uint32_t *)tmp);
}

/**
  * @}
  */

/** @defgroup RTC_Group11 RTC Tamper and TimeStamp Pins Selection and Output Type Config configuration functions
 *  @brief   RTC Tamper and TimeStamp Pins Selection and Output Type Config 
 *           configuration functions  
 *
@verbatim   
 ===============================================================================
  RTC Tamper and TimeStamp Pins Selection and Output Type Config configuration 
  functions 
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Selects the RTC Tamper Pin.
  * @param  RTC_TamperPin: specifies the RTC Tamper Pin.
  *          This parameter can be one of the following values:
  *            @arg RTC_TamperPin_PC13: PC13 is selected as RTC Tamper Pin.
  *            @arg RTC_TamperPin_PI8: PI8 is selected as RTC Tamper Pin.    
  * @retval None
  */
void RTC_TamperPinSelection(uint32_t RTC_TamperPin)
{
  /* Check the parameters */
  assert_param(IS_RTC_TAMPER_PIN(RTC_TamperPin));
  
  RTC->TAFCR &= (uint32_t)~(RTC_TAFCR_TAMPINSEL);
  RTC->TAFCR |= (uint32_t)(RTC_TamperPin);  
}

/**
  * @brief  Selects the RTC TimeStamp Pin.
  * @param  RTC_TimeStampPin: specifies the RTC TimeStamp Pin.
  *          This parameter can be one of the following values:
  *            @arg RTC_TimeStampPin_PC13: PC13 is selected as RTC TimeStamp Pin.
  *            @arg RTC_TimeStampPin_PI8: PI8 is selected as RTC TimeStamp Pin.    
  * @retval None
  */
void RTC_TimeStampPinSelection(uint32_t RTC_TimeStampPin)
{
  /* Check the parameters */
  assert_param(IS_RTC_TIMESTAMP_PIN(RTC_TimeStampPin));
  
  RTC->TAFCR &= (uint32_t)~(RTC_TAFCR_TSINSEL);
  RTC->TAFCR |= (uint32_t)(RTC_TimeStampPin);  
}

/**
  * @brief  Configures the RTC Output Pin mode. 
  * @param  RTC_OutputType: specifies the RTC Output (PC13) pin mode.
  *          This parameter can be one of the following values:
  *            @arg RTC_OutputType_OpenDrain: RTC Output (PC13) is configured in 
  *                                    Open Drain mode.
  *            @arg RTC_OutputType_PushPull:  RTC Output (PC13) is configured in 
  *                                    Push Pull mode.    
  * @retval None
  */
void RTC_OutputTypeConfig(uint32_t RTC_OutputType)
{
  /* Check the parameters */
  assert_param(IS_RTC_OUTPUT_TYPE(RTC_OutputType));
  
  RTC->TAFCR &= (uint32_t)~(RTC_TAFCR_ALARMOUTTYPE);
  RTC->TAFCR |= (uint32_t)(RTC_OutputType);  
}

/**
  * @}
  */

/** @defgroup RTC_Group12 Shift control synchronisation functions
 *  @brief   Shift control synchronisation functions 
 *
@verbatim   
 ===============================================================================
                   Shift control synchronisation functions
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Configures the Synchronization Shift Control Settings.
  * @note   When REFCKON is set, firmware must not write to Shift control register 
  * @param  RTC_ShiftAdd1S : Select to add or not 1 second to the time Calendar.
  *   This parameter can be one of the following values :
  *     @arg RTC_ShiftAdd1S_Set  : Add one second to the clock calendar. 
  *     @arg RTC_ShiftAdd1S_Reset: No effect.
  * @param  RTC_ShiftSubFS: Select the number of Second Fractions to Substitute.
  *         This parameter can be one any value from 0 to 0x7FFF.
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: RTC Shift registers are configured
  *          - ERROR: RTC Shift registers are not configured
*/
ErrorStatus RTC_SynchroShiftConfig(uint32_t RTC_ShiftAdd1S, uint32_t RTC_ShiftSubFS)
{
  ErrorStatus status = ERROR;
  uint32_t shpfcount = 0;

  /* Check the parameters */
  assert_param(IS_RTC_SHIFT_ADD1S(RTC_ShiftAdd1S));
  assert_param(IS_RTC_SHIFT_SUBFS(RTC_ShiftSubFS));

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;
  
  /* Check if a Shift is pending*/
  if ((RTC->ISR & RTC_ISR_SHPF) != RESET)
  {
    /* Wait until the shift is completed*/
    while (((RTC->ISR & RTC_ISR_SHPF) != RESET) && (shpfcount != SHPF_TIMEOUT))
    {
      shpfcount++;
    }
  }

  /* Check if the Shift pending is completed or if there is no Shift operation at all*/
  if ((RTC->ISR & RTC_ISR_SHPF) == RESET)
  {
    /* check if the reference clock detection is disabled */
    if((RTC->CR & RTC_CR_REFCKON) == RESET)
    {
      /* Configure the Shift settings */
      RTC->SHIFTR = (uint32_t)(uint32_t)(RTC_ShiftSubFS) | (uint32_t)(RTC_ShiftAdd1S);
    
      if(RTC_WaitForSynchro() == ERROR)
      {
        status = ERROR;
      }
      else
      {
        status = SUCCESS;
      }
    }
    else
    {
      status = ERROR;
    }
  }
  else
  {
    status = ERROR;
  }

  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF;
  
  return (ErrorStatus)(status);
}

/**
  * @}
  */

/** @defgroup RTC_Group13 Interrupts and flags management functions
 *  @brief   Interrupts and flags management functions  
 *
@verbatim   
 ===============================================================================
                       Interrupts and flags management functions
 ===============================================================================  
 All RTC interrupts are connected to the EXTI controller.
 
 - To enable the RTC Alarm interrupt, the following sequence is required:
   - Configure and enable the EXTI Line 17 in interrupt mode and select the rising 
     edge sensitivity using the EXTI_Init() function.
   - Configure and enable the RTC_Alarm IRQ channel in the NVIC using the NVIC_Init()
     function.
   - Configure the RTC to generate RTC alarms (Alarm A and/or Alarm B) using
     the RTC_SetAlarm() and RTC_AlarmCmd() functions.

 - To enable the RTC Wakeup interrupt, the following sequence is required:
   - Configure and enable the EXTI Line 22 in interrupt mode and select the rising 
     edge sensitivity using the EXTI_Init() function.
   - Configure and enable the RTC_WKUP IRQ channel in the NVIC using the NVIC_Init()
     function.
   - Configure the RTC to generate the RTC wakeup timer event using the 
     RTC_WakeUpClockConfig(), RTC_SetWakeUpCounter() and RTC_WakeUpCmd() functions.

 - To enable the RTC Tamper interrupt, the following sequence is required:
   - Configure and enable the EXTI Line 21 in interrupt mode and select the rising 
     edge sensitivity using the EXTI_Init() function.
   - Configure and enable the TAMP_STAMP IRQ channel in the NVIC using the NVIC_Init()
     function.
   - Configure the RTC to detect the RTC tamper event using the 
     RTC_TamperTriggerConfig() and RTC_TamperCmd() functions.

 - To enable the RTC TimeStamp interrupt, the following sequence is required:
   - Configure and enable the EXTI Line 21 in interrupt mode and select the rising 
     edge sensitivity using the EXTI_Init() function.
   - Configure and enable the TAMP_STAMP IRQ channel in the NVIC using the NVIC_Init()
     function.
   - Configure the RTC to detect the RTC time-stamp event using the 
     RTC_TimeStampCmd() functions.

@endverbatim
  * @{
  */

/**
  * @brief  Enables or disables the specified RTC interrupts.
  * @param  RTC_IT: specifies the RTC interrupt sources to be enabled or disabled. 
  *          This parameter can be any combination of the following values:
  *            @arg RTC_IT_TS:  Time Stamp interrupt mask
  *            @arg RTC_IT_WUT:  WakeUp Timer interrupt mask
  *            @arg RTC_IT_ALRB:  Alarm B interrupt mask
  *            @arg RTC_IT_ALRA:  Alarm A interrupt mask
  *            @arg RTC_IT_TAMP: Tamper event interrupt mask
  * @param  NewState: new state of the specified RTC interrupts.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void RTC_ITConfig(uint32_t RTC_IT, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_RTC_CONFIG_IT(RTC_IT));
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  /* Disable the write protection for RTC registers */
  RTC->WPR = 0xCA;
  RTC->WPR = 0x53;

  if (NewState != DISABLE)
  {
    /* Configure the Interrupts in the RTC_CR register */
    RTC->CR |= (uint32_t)(RTC_IT & ~RTC_TAFCR_TAMPIE);
    /* Configure the Tamper Interrupt in the RTC_TAFCR */
    RTC->TAFCR |= (uint32_t)(RTC_IT & RTC_TAFCR_TAMPIE);
  }
  else
  {
    /* Configure the Interrupts in the RTC_CR register */
    RTC->CR &= (uint32_t)~(RTC_IT & (uint32_t)~RTC_TAFCR_TAMPIE);
    /* Configure the Tamper Interrupt in the RTC_TAFCR */
    RTC->TAFCR &= (uint32_t)~(RTC_IT & RTC_TAFCR_TAMPIE);
  }
  /* Enable the write protection for RTC registers */
  RTC->WPR = 0xFF; 
}

/**
  * @brief  Checks whether the specified RTC flag is set or not.
  * @param  RTC_FLAG: specifies the flag to check.
  *          This parameter can be one of the following values:
  *            @arg RTC_FLAG_TAMP1F: Tamper 1 event flag
  *            @arg RTC_FLAG_TSOVF: Time Stamp OverFlow flag
  *            @arg RTC_FLAG_TSF: Time Stamp event flag
  *            @arg RTC_FLAG_WUTF: WakeUp Timer flag
  *            @arg RTC_FLAG_ALRBF: Alarm B flag
  *            @arg RTC_FLAG_ALRAF: Alarm A flag
  *            @arg RTC_FLAG_INITF: Initialization mode flag
  *            @arg RTC_FLAG_RSF: Registers Synchronized flag
  *            @arg RTC_FLAG_INITS: Registers Configured flag
  *            @arg RTC_FLAG_WUTWF: WakeUp Timer Write flag
  *            @arg RTC_FLAG_ALRBWF: Alarm B Write flag
  *            @arg RTC_FLAG_ALRAWF: Alarm A write flag
  * @retval The new state of RTC_FLAG (SET or RESET).
  */
FlagStatus RTC_GetFlagStatus(uint32_t RTC_FLAG)
{
  FlagStatus bitstatus = RESET;
  uint32_t tmpreg = 0;
  
  /* Check the parameters */
  assert_param(IS_RTC_GET_FLAG(RTC_FLAG));
  
  /* Get all the flags */
  tmpreg = (uint32_t)(RTC->ISR & RTC_FLAGS_MASK);
  
  /* Return the status of the flag */
  if ((tmpreg & RTC_FLAG) != (uint32_t)RESET)
  {
    bitstatus = SET;
  }
  else
  {
    bitstatus = RESET;
  }
  return bitstatus;
}

/**
  * @brief  Clears the RTC's pending flags.
  * @param  RTC_FLAG: specifies the RTC flag to clear.
  *          This parameter can be any combination of the following values:
  *            @arg RTC_FLAG_TAMP1F: Tamper 1 event flag
  *            @arg RTC_FLAG_TSOVF: Time Stamp Overflow flag 
  *            @arg RTC_FLAG_TSF: Time Stamp event flag
  *            @arg RTC_FLAG_WUTF: WakeUp Timer flag
  *            @arg RTC_FLAG_ALRBF: Alarm B flag
  *            @arg RTC_FLAG_ALRAF: Alarm A flag
  *            @arg RTC_FLAG_RSF: Registers Synchronized flag
  * @retval None
  */
void RTC_ClearFlag(uint32_t RTC_FLAG)
{
  /* Check the parameters */
  assert_param(IS_RTC_CLEAR_FLAG(RTC_FLAG));

  /* Clear the Flags in the RTC_ISR register */
  RTC->ISR = (uint32_t)((uint32_t)(~((RTC_FLAG | RTC_ISR_INIT)& 0x0000FFFF) | (uint32_t)(RTC->ISR & RTC_ISR_INIT)));  
}

/**
  * @brief  Checks whether the specified RTC interrupt has occurred or not.
  * @param  RTC_IT: specifies the RTC interrupt source to check.
  *          This parameter can be one of the following values:
  *            @arg RTC_IT_TS: Time Stamp interrupt 
  *            @arg RTC_IT_WUT: WakeUp Timer interrupt 
  *            @arg RTC_IT_ALRB: Alarm B interrupt 
  *            @arg RTC_IT_ALRA: Alarm A interrupt 
  *            @arg RTC_IT_TAMP1: Tamper 1 event interrupt 
  * @retval The new state of RTC_IT (SET or RESET).
  */
ITStatus RTC_GetITStatus(uint32_t RTC_IT)
{
  ITStatus bitstatus = RESET;
  uint32_t tmpreg = 0, enablestatus = 0;
 
  /* Check the parameters */
  assert_param(IS_RTC_GET_IT(RTC_IT));
  
  /* Get the TAMPER Interrupt enable bit and pending bit */
  tmpreg = (uint32_t)(RTC->TAFCR & (RTC_TAFCR_TAMPIE));
 
  /* Get the Interrupt enable Status */
  enablestatus = (uint32_t)((RTC->CR & RTC_IT) | (tmpreg & (RTC_IT >> 15)));
  
  /* Get the Interrupt pending bit */
  tmpreg = (uint32_t)((RTC->ISR & (uint32_t)(RTC_IT >> 4)));
  
  /* Get the status of the Interrupt */
  if ((enablestatus != (uint32_t)RESET) && ((tmpreg & 0x0000FFFF) != (uint32_t)RESET))
  {
    bitstatus = SET;
  }
  else
  {
    bitstatus = RESET;
  }
  return bitstatus;
}

/**
  * @brief  Clears the RTC's interrupt pending bits.
  * @param  RTC_IT: specifies the RTC interrupt pending bit to clear.
  *          This parameter can be any combination of the following values:
  *            @arg RTC_IT_TS: Time Stamp interrupt 
  *            @arg RTC_IT_WUT: WakeUp Timer interrupt 
  *            @arg RTC_IT_ALRB: Alarm B interrupt 
  *            @arg RTC_IT_ALRA: Alarm A interrupt 
  *            @arg RTC_IT_TAMP1: Tamper 1 event interrupt 
  * @retval None
  */
void RTC_ClearITPendingBit(uint32_t RTC_IT)
{
  uint32_t tmpreg = 0;

  /* Check the parameters */
  assert_param(IS_RTC_CLEAR_IT(RTC_IT));

  /* Get the RTC_ISR Interrupt pending bits mask */
  tmpreg = (uint32_t)(RTC_IT >> 4);

  /* Clear the interrupt pending bits in the RTC_ISR register */
  RTC->ISR = (uint32_t)((uint32_t)(~((tmpreg | RTC_ISR_INIT)& 0x0000FFFF) | (uint32_t)(RTC->ISR & RTC_ISR_INIT))); 
}

/**
  * @}
  */

/**
  * @brief  Converts a 2 digit decimal to BCD format.
  * @param  Value: Byte to be converted.
  * @retval Converted byte
  */
static uint8_t RTC_ByteToBcd2(uint8_t Value)
{
  uint8_t bcdhigh = 0;
  
  while (Value >= 10)
  {
    bcdhigh++;
    Value -= 10;
  }
  
  return  ((uint8_t)(bcdhigh << 4) | Value);
}

/**
  * @brief  Convert from 2 digit BCD to Binary.
  * @param  Value: BCD value to be converted.
  * @retval Converted word
  */
static uint8_t RTC_Bcd2ToByte(uint8_t Value)
{
  uint8_t tmp = 0;
  tmp = ((uint8_t)(Value & (uint8_t)0xF0) >> (uint8_t)0x4) * 10;
  return (tmp + (Value & (uint8_t)0x0F));
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

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
