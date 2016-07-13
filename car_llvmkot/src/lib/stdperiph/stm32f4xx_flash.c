/**
  ******************************************************************************
  * @file    stm32f4xx_flash.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file provides firmware functions to manage the following 
  *          functionalities of the FLASH peripheral:
  *            - FLASH Interface configuration
  *            - FLASH Memory Programming
  *            - Option Bytes Programming
  *            - Interrupts and flags management
  *  
  *  @verbatim
  *  
  *          ===================================================================
  *                                 How to use this driver
  *          ===================================================================
  *                           
  *          This driver provides functions to configure and program the FLASH 
  *          memory of all STM32F4xx devices.
  *          These functions are split in 4 groups:
  * 
  *           1. FLASH Interface configuration functions: this group includes the
  *              management of the following features:
  *                    - Set the latency
  *                    - Enable/Disable the prefetch buffer
  *                    - Enable/Disable the Instruction cache and the Data cache
  *                    - Reset the Instruction cache and the Data cache
  *  
  *           2. FLASH Memory Programming functions: this group includes all needed
  *              functions to erase and program the main memory:
  *                    - Lock and Unlock the FLASH interface
  *                    - Erase function: Erase sector, erase all sectors
  *                    - Program functions: byte, half word, word and double word
  *  
  *           3. Option Bytes Programming functions: this group includes all needed
  *              functions to manage the Option Bytes:
  *                    - Set/Reset the write protection
  *                    - Set the Read protection Level
  *                    - Set the BOR level
  *                    - Program the user Option Bytes
  *                    - Launch the Option Bytes loader
  *  
  *           4. Interrupts and flags management functions: this group 
  *              includes all needed functions to:
  *                    - Enable/Disable the FLASH interrupt sources
  *                    - Get flags status
  *                    - Clear flags
  *                    - Get FLASH operation status
  *                    - Wait for last FLASH operation
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
#include "stm32f4xx_flash.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup FLASH 
  * @brief FLASH driver modules
  * @{
  */ 

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/ 
#define SECTOR_MASK               ((uint32_t)0xFFFFFF07)

/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
/* Private functions ---------------------------------------------------------*/

/** @defgroup FLASH_Private_Functions
  * @{
  */ 

/** @defgroup FLASH_Group1 FLASH Interface configuration functions
  *  @brief   FLASH Interface configuration functions 
 *

@verbatim   
 ===============================================================================
                       FLASH Interface configuration functions
 ===============================================================================

   This group includes the following functions:
    - void FLASH_SetLatency(uint32_t FLASH_Latency)
       To correctly read data from FLASH memory, the number of wait states (LATENCY) 
       must be correctly programmed according to the frequency of the CPU clock 
      (HCLK) and the supply voltage of the device.
 +-------------------------------------------------------------------------------------+     
 | Latency       |                HCLK clock frequency (MHz)                           |
 |               |---------------------------------------------------------------------|     
 |               | voltage range  | voltage range  | voltage range   | voltage range   |
 |               | 2.7 V - 3.6 V  | 2.4 V - 2.7 V  | 2.1 V - 2.4 V   | 1.8 V - 2.1 V   |
 |---------------|----------------|----------------|-----------------|-----------------|              
 |0WS(1CPU cycle)|0 < HCLK <= 30  |0 < HCLK <= 24  |0 < HCLK <= 18   |0 < HCLK <= 16   |
 |---------------|----------------|----------------|-----------------|-----------------|   
 |1WS(2CPU cycle)|30 < HCLK <= 60 |24 < HCLK <= 48 |18 < HCLK <= 36  |16 < HCLK <= 32  | 
 |---------------|----------------|----------------|-----------------|-----------------|   
 |2WS(3CPU cycle)|60 < HCLK <= 90 |48 < HCLK <= 72 |36 < HCLK <= 54  |32 < HCLK <= 48  |
 |---------------|----------------|----------------|-----------------|-----------------| 
 |3WS(4CPU cycle)|90 < HCLK <= 120|72 < HCLK <= 96 |54 < HCLK <= 72  |48 < HCLK <= 64  |
 |---------------|----------------|----------------|-----------------|-----------------| 
 |4WS(5CPU cycle)|120< HCLK <= 150|96 < HCLK <= 120|72 < HCLK <= 90  |64 < HCLK <= 80  |
 |---------------|----------------|----------------|-----------------|-----------------| 
 |5WS(6CPU cycle)|120< HCLK <= 168|120< HCLK <= 144|90 < HCLK <= 108 |80 < HCLK <= 96  | 
 |---------------|----------------|----------------|-----------------|-----------------| 
 |6WS(7CPU cycle)|      NA        |144< HCLK <= 168|108 < HCLK <= 120|96 < HCLK <= 112 | 
 |---------------|----------------|----------------|-----------------|-----------------| 
 |7WS(8CPU cycle)|      NA        |      NA        |120 < HCLK <= 138|112 < HCLK <= 120| 
 |***************|****************|****************|*****************|*****************|*****************************+
 |               | voltage range  | voltage range  | voltage range   | voltage range   | voltage range 2.7 V - 3.6 V |
 |               | 2.7 V - 3.6 V  | 2.4 V - 2.7 V  | 2.1 V - 2.4 V   | 1.8 V - 2.1 V   | with External Vpp = 9V      |
 |---------------|----------------|----------------|-----------------|-----------------|-----------------------------| 
 |Max Parallelism|      x32       |               x16                |       x8        |          x64                |              
 |---------------|----------------|----------------|-----------------|-----------------|-----------------------------|   
 |PSIZE[1:0]     |      10        |               01                 |       00        |           11                |
 +-------------------------------------------------------------------------------------------------------------------+  
   @note When VOS bit (in PWR_CR register) is reset to '0’, the maximum value of HCLK is 144 MHz.
         You can use PWR_MainRegulatorModeConfig() function to set or reset this bit.
             
    - void FLASH_PrefetchBufferCmd(FunctionalState NewState)
    - void FLASH_InstructionCacheCmd(FunctionalState NewState)
    - void FLASH_DataCacheCmd(FunctionalState NewState)
    - void FLASH_InstructionCacheReset(void)
    - void FLASH_DataCacheReset(void)
   
   The unlock sequence is not needed for these functions.
 
@endverbatim
  * @{
  */
 
/**
  * @brief  Sets the code latency value.
  * @param  FLASH_Latency: specifies the FLASH Latency value.
  *          This parameter can be one of the following values:
  *            @arg FLASH_Latency_0: FLASH Zero Latency cycle
  *            @arg FLASH_Latency_1: FLASH One Latency cycle
  *            @arg FLASH_Latency_2: FLASH Two Latency cycles
  *            @arg FLASH_Latency_3: FLASH Three Latency cycles
  *            @arg FLASH_Latency_4: FLASH Four Latency cycles 
  *            @arg FLASH_Latency_5: FLASH Five Latency cycles 
  *            @arg FLASH_Latency_6: FLASH Six Latency cycles
  *            @arg FLASH_Latency_7: FLASH Seven Latency cycles      
  * @retval None
  */
void FLASH_SetLatency(uint32_t FLASH_Latency)
{
  /* Check the parameters */
  assert_param(IS_FLASH_LATENCY(FLASH_Latency));
  
  /* Perform Byte access to FLASH_ACR[8:0] to set the Latency value */
  *(__IO uint8_t *)ACR_BYTE0_ADDRESS = (uint8_t)FLASH_Latency;
}

/**
  * @brief  Enables or disables the Prefetch Buffer.
  * @param  NewState: new state of the Prefetch Buffer.
  *          This parameter  can be: ENABLE or DISABLE.
  * @retval None
  */
void FLASH_PrefetchBufferCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  /* Enable or disable the Prefetch Buffer */
  if(NewState != DISABLE)
  {
    FLASH->ACR |= FLASH_ACR_PRFTEN;
  }
  else
  {
    FLASH->ACR &= (~FLASH_ACR_PRFTEN);
  }
}

/**
  * @brief  Enables or disables the Instruction Cache feature.
  * @param  NewState: new state of the Instruction Cache.
  *          This parameter  can be: ENABLE or DISABLE.
  * @retval None
  */
void FLASH_InstructionCacheCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if(NewState != DISABLE)
  {
    FLASH->ACR |= FLASH_ACR_ICEN;
  }
  else
  {
    FLASH->ACR &= (~FLASH_ACR_ICEN);
  }
}

/**
  * @brief  Enables or disables the Data Cache feature.
  * @param  NewState: new state of the Data Cache.
  *          This parameter  can be: ENABLE or DISABLE.
  * @retval None
  */
void FLASH_DataCacheCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if(NewState != DISABLE)
  {
    FLASH->ACR |= FLASH_ACR_DCEN;
  }
  else
  {
    FLASH->ACR &= (~FLASH_ACR_DCEN);
  }
}

/**
  * @brief  Resets the Instruction Cache.
  * @note   This function must be used only when the Instruction Cache is disabled.  
  * @param  None
  * @retval None
  */
void FLASH_InstructionCacheReset(void)
{
  FLASH->ACR |= FLASH_ACR_ICRST;
}

/**
  * @brief  Resets the Data Cache.
  * @note   This function must be used only when the Data Cache is disabled.  
  * @param  None
  * @retval None
  */
void FLASH_DataCacheReset(void)
{
  FLASH->ACR |= FLASH_ACR_DCRST;
}

/**
  * @}
  */

/** @defgroup FLASH_Group2 FLASH Memory Programming functions
 *  @brief   FLASH Memory Programming functions
 *
@verbatim   
 ===============================================================================
                      FLASH Memory Programming functions
 ===============================================================================   

   This group includes the following functions:
    - void FLASH_Unlock(void)
    - void FLASH_Lock(void)
    - FLASH_Status FLASH_EraseSector(uint32_t FLASH_Sector, uint8_t VoltageRange)
    - FLASH_Status FLASH_EraseAllSectors(uint8_t VoltageRange)
    - FLASH_Status FLASH_ProgramDoubleWord(uint32_t Address, uint64_t Data)
    - FLASH_Status FLASH_ProgramWord(uint32_t Address, uint32_t Data)
    - FLASH_Status FLASH_ProgramHalfWord(uint32_t Address, uint16_t Data)
    - FLASH_Status FLASH_ProgramByte(uint32_t Address, uint8_t Data)
   
   Any operation of erase or program should follow these steps:
   1. Call the FLASH_Unlock() function to enable the FLASH control register access

   2. Call the desired function to erase sector(s) or program data

   3. Call the FLASH_Lock() function to disable the FLASH control register access
      (recommended to protect the FLASH memory against possible unwanted operation)
    
@endverbatim
  * @{
  */

/**
  * @brief  Unlocks the FLASH control register access
  * @param  None
  * @retval None
  */
void FLASH_Unlock(void)
{
  if((FLASH->CR & FLASH_CR_LOCK) != RESET)
  {
    /* Authorize the FLASH Registers access */
    FLASH->KEYR = FLASH_KEY1;
    FLASH->KEYR = FLASH_KEY2;
  }  
}

/**
  * @brief  Locks the FLASH control register access
  * @param  None
  * @retval None
  */
void FLASH_Lock(void)
{
  /* Set the LOCK Bit to lock the FLASH Registers access */
  FLASH->CR |= FLASH_CR_LOCK;
}

/**
  * @brief  Erases a specified FLASH Sector.
  *   
  * @param  FLASH_Sector: The Sector number to be erased.
  *          This parameter can be a value between FLASH_Sector_0 and FLASH_Sector_11
  *    
  * @param  VoltageRange: The device voltage range which defines the erase parallelism.  
  *          This parameter can be one of the following values:
  *            @arg VoltageRange_1: when the device voltage range is 1.8V to 2.1V, 
  *                                  the operation will be done by byte (8-bit) 
  *            @arg VoltageRange_2: when the device voltage range is 2.1V to 2.7V,
  *                                  the operation will be done by half word (16-bit)
  *            @arg VoltageRange_3: when the device voltage range is 2.7V to 3.6V,
  *                                  the operation will be done by word (32-bit)
  *            @arg VoltageRange_4: when the device voltage range is 2.7V to 3.6V + External Vpp, 
  *                                  the operation will be done by double word (64-bit)
  *       
  * @retval FLASH Status: The returned value can be: FLASH_BUSY, FLASH_ERROR_PROGRAM,
  *                       FLASH_ERROR_WRP, FLASH_ERROR_OPERATION or FLASH_COMPLETE.
  */
FLASH_Status FLASH_EraseSector(uint32_t FLASH_Sector, uint8_t VoltageRange)
{
  uint32_t tmp_psize = 0x0;
  FLASH_Status status = FLASH_COMPLETE;

  /* Check the parameters */
  assert_param(IS_FLASH_SECTOR(FLASH_Sector));
  assert_param(IS_VOLTAGERANGE(VoltageRange));
  
  if(VoltageRange == VoltageRange_1)
  {
     tmp_psize = FLASH_PSIZE_BYTE;
  }
  else if(VoltageRange == VoltageRange_2)
  {
    tmp_psize = FLASH_PSIZE_HALF_WORD;
  }
  else if(VoltageRange == VoltageRange_3)
  {
    tmp_psize = FLASH_PSIZE_WORD;
  }
  else
  {
    tmp_psize = FLASH_PSIZE_DOUBLE_WORD;
  }
  /* Wait for last operation to be completed */
  status = FLASH_WaitForLastOperation();
  
  if(status == FLASH_COMPLETE)
  { 
    /* if the previous operation is completed, proceed to erase the sector */
    FLASH->CR &= CR_PSIZE_MASK;
    FLASH->CR |= tmp_psize;
    FLASH->CR &= SECTOR_MASK;
    FLASH->CR |= FLASH_CR_SER | FLASH_Sector;
    FLASH->CR |= FLASH_CR_STRT;
    
    /* Wait for last operation to be completed */
    status = FLASH_WaitForLastOperation();
    
    /* if the erase operation is completed, disable the SER Bit */
    FLASH->CR &= (~FLASH_CR_SER);
    FLASH->CR &= SECTOR_MASK; 
  }
  /* Return the Erase Status */
  return status;
}

/**
  * @brief  Erases all FLASH Sectors.
  *    
  * @param  VoltageRange: The device voltage range which defines the erase parallelism.  
  *          This parameter can be one of the following values:
  *            @arg VoltageRange_1: when the device voltage range is 1.8V to 2.1V, 
  *                                  the operation will be done by byte (8-bit) 
  *            @arg VoltageRange_2: when the device voltage range is 2.1V to 2.7V,
  *                                  the operation will be done by half word (16-bit)
  *            @arg VoltageRange_3: when the device voltage range is 2.7V to 3.6V,
  *                                  the operation will be done by word (32-bit)
  *            @arg VoltageRange_4: when the device voltage range is 2.7V to 3.6V + External Vpp, 
  *                                  the operation will be done by double word (64-bit)
  *       
  * @retval FLASH Status: The returned value can be: FLASH_BUSY, FLASH_ERROR_PROGRAM,
  *                       FLASH_ERROR_WRP, FLASH_ERROR_OPERATION or FLASH_COMPLETE.
  */
FLASH_Status FLASH_EraseAllSectors(uint8_t VoltageRange)
{
  uint32_t tmp_psize = 0x0;
  FLASH_Status status = FLASH_COMPLETE;
  
  /* Wait for last operation to be completed */
  status = FLASH_WaitForLastOperation();
  assert_param(IS_VOLTAGERANGE(VoltageRange));
  
  if(VoltageRange == VoltageRange_1)
  {
     tmp_psize = FLASH_PSIZE_BYTE;
  }
  else if(VoltageRange == VoltageRange_2)
  {
    tmp_psize = FLASH_PSIZE_HALF_WORD;
  }
  else if(VoltageRange == VoltageRange_3)
  {
    tmp_psize = FLASH_PSIZE_WORD;
  }
  else
  {
    tmp_psize = FLASH_PSIZE_DOUBLE_WORD;
  }  
  if(status == FLASH_COMPLETE)
  {
    /* if the previous operation is completed, proceed to erase all sectors */
     FLASH->CR &= CR_PSIZE_MASK;
     FLASH->CR |= tmp_psize;
     FLASH->CR |= FLASH_CR_MER;
     FLASH->CR |= FLASH_CR_STRT;
    
    /* Wait for last operation to be completed */
    status = FLASH_WaitForLastOperation();

    /* if the erase operation is completed, disable the MER Bit */
    FLASH->CR &= (~FLASH_CR_MER);

  }   
  /* Return the Erase Status */
  return status;
}

/**
  * @brief  Programs a double word (64-bit) at a specified address.
  * @note   This function must be used when the device voltage range is from
  *         2.7V to 3.6V and an External Vpp is present.           
  * @param  Address: specifies the address to be programmed.
  * @param  Data: specifies the data to be programmed.
  * @retval FLASH Status: The returned value can be: FLASH_BUSY, FLASH_ERROR_PROGRAM,
  *                       FLASH_ERROR_WRP, FLASH_ERROR_OPERATION or FLASH_COMPLETE.
  */
FLASH_Status FLASH_ProgramDoubleWord(uint32_t Address, uint64_t Data)
{
  FLASH_Status status = FLASH_COMPLETE;

  /* Check the parameters */
  assert_param(IS_FLASH_ADDRESS(Address));

  /* Wait for last operation to be completed */
  status = FLASH_WaitForLastOperation();
  
  if(status == FLASH_COMPLETE)
  {
    /* if the previous operation is completed, proceed to program the new data */
    FLASH->CR &= CR_PSIZE_MASK;
    FLASH->CR |= FLASH_PSIZE_DOUBLE_WORD;
    FLASH->CR |= FLASH_CR_PG;
  
    *(__IO uint64_t*)Address = Data;
        
    /* Wait for last operation to be completed */
    status = FLASH_WaitForLastOperation();

    /* if the program operation is completed, disable the PG Bit */
    FLASH->CR &= (~FLASH_CR_PG);
  } 
  /* Return the Program Status */
  return status;
}

/**
  * @brief  Programs a word (32-bit) at a specified address.
  * @param  Address: specifies the address to be programmed.
  *         This parameter can be any address in Program memory zone or in OTP zone.  
  * @note   This function must be used when the device voltage range is from 2.7V to 3.6V. 
  * @param  Data: specifies the data to be programmed.
  * @retval FLASH Status: The returned value can be: FLASH_BUSY, FLASH_ERROR_PROGRAM,
  *                       FLASH_ERROR_WRP, FLASH_ERROR_OPERATION or FLASH_COMPLETE.
  */
FLASH_Status FLASH_ProgramWord(uint32_t Address, uint32_t Data)
{
  FLASH_Status status = FLASH_COMPLETE;

  /* Check the parameters */
  assert_param(IS_FLASH_ADDRESS(Address));

  /* Wait for last operation to be completed */
  status = FLASH_WaitForLastOperation();
  
  if(status == FLASH_COMPLETE)
  {
    /* if the previous operation is completed, proceed to program the new data */
    FLASH->CR &= CR_PSIZE_MASK;
    FLASH->CR |= FLASH_PSIZE_WORD;
    FLASH->CR |= FLASH_CR_PG;
  
    *(__IO uint32_t*)Address = Data;
        
    /* Wait for last operation to be completed */
    status = FLASH_WaitForLastOperation();

    /* if the program operation is completed, disable the PG Bit */
    FLASH->CR &= (~FLASH_CR_PG);
  } 
  /* Return the Program Status */
  return status;
}

/**
  * @brief  Programs a half word (16-bit) at a specified address. 
  * @note   This function must be used when the device voltage range is from 2.1V to 3.6V.               
  * @param  Address: specifies the address to be programmed.
  *         This parameter can be any address in Program memory zone or in OTP zone.  
  * @param  Data: specifies the data to be programmed.
  * @retval FLASH Status: The returned value can be: FLASH_BUSY, FLASH_ERROR_PROGRAM,
  *                       FLASH_ERROR_WRP, FLASH_ERROR_OPERATION or FLASH_COMPLETE.
  */
FLASH_Status FLASH_ProgramHalfWord(uint32_t Address, uint16_t Data)
{
  FLASH_Status status = FLASH_COMPLETE;

  /* Check the parameters */
  assert_param(IS_FLASH_ADDRESS(Address));

  /* Wait for last operation to be completed */
  status = FLASH_WaitForLastOperation();
  
  if(status == FLASH_COMPLETE)
  {
    /* if the previous operation is completed, proceed to program the new data */
    FLASH->CR &= CR_PSIZE_MASK;
    FLASH->CR |= FLASH_PSIZE_HALF_WORD;
    FLASH->CR |= FLASH_CR_PG;
  
    *(__IO uint16_t*)Address = Data;
        
    /* Wait for last operation to be completed */
    status = FLASH_WaitForLastOperation();

    /* if the program operation is completed, disable the PG Bit */
    FLASH->CR &= (~FLASH_CR_PG);
  } 
  /* Return the Program Status */
  return status;
}

/**
  * @brief  Programs a byte (8-bit) at a specified address.
  * @note   This function can be used within all the device supply voltage ranges.               
  * @param  Address: specifies the address to be programmed.
  *         This parameter can be any address in Program memory zone or in OTP zone.  
  * @param  Data: specifies the data to be programmed.
  * @retval FLASH Status: The returned value can be: FLASH_BUSY, FLASH_ERROR_PROGRAM,
  *                       FLASH_ERROR_WRP, FLASH_ERROR_OPERATION or FLASH_COMPLETE.
  */
FLASH_Status FLASH_ProgramByte(uint32_t Address, uint8_t Data)
{
  FLASH_Status status = FLASH_COMPLETE;

  /* Check the parameters */
  assert_param(IS_FLASH_ADDRESS(Address));

  /* Wait for last operation to be completed */
  status = FLASH_WaitForLastOperation();
  
  if(status == FLASH_COMPLETE)
  {
    /* if the previous operation is completed, proceed to program the new data */
    FLASH->CR &= CR_PSIZE_MASK;
    FLASH->CR |= FLASH_PSIZE_BYTE;
    FLASH->CR |= FLASH_CR_PG;
  
    *(__IO uint8_t*)Address = Data;
        
    /* Wait for last operation to be completed */
    status = FLASH_WaitForLastOperation();

    /* if the program operation is completed, disable the PG Bit */
    FLASH->CR &= (~FLASH_CR_PG);
  } 

  /* Return the Program Status */
  return status;
}

/**
  * @}
  */

/** @defgroup FLASH_Group3 Option Bytes Programming functions
 *  @brief   Option Bytes Programming functions 
 *
@verbatim   
 ===============================================================================
                        Option Bytes Programming functions
 ===============================================================================  
 
   This group includes the following functions:
   - void FLASH_OB_Unlock(void)
   - void FLASH_OB_Lock(void)
   - void FLASH_OB_WRPConfig(uint32_t OB_WRP, FunctionalState NewState)
   - void FLASH_OB_RDPConfig(uint8_t OB_RDP)
   - void FLASH_OB_UserConfig(uint8_t OB_IWDG, uint8_t OB_STOP, uint8_t OB_STDBY)
   - void FLASH_OB_BORConfig(uint8_t OB_BOR)
   - FLASH_Status FLASH_ProgramOTP(uint32_t Address, uint32_t Data)							
   - FLASH_Status FLASH_OB_Launch(void)
   - uint32_t FLASH_OB_GetUser(void)						
   - uint8_t FLASH_OB_GetWRP(void)						
   - uint8_t FLASH_OB_GetRDP(void)							
   - uint8_t FLASH_OB_GetBOR(void)
   
   Any operation of erase or program should follow these steps:
   1. Call the FLASH_OB_Unlock() function to enable the FLASH option control register access

   2. Call one or several functions to program the desired Option Bytes:
      - void FLASH_OB_WRPConfig(uint32_t OB_WRP, FunctionalState NewState) => to Enable/Disable 
        the desired sector write protection
      - void FLASH_OB_RDPConfig(uint8_t OB_RDP) => to set the desired read Protection Level
      - void FLASH_OB_UserConfig(uint8_t OB_IWDG, uint8_t OB_STOP, uint8_t OB_STDBY) => to configure 
        the user Option Bytes.
      - void FLASH_OB_BORConfig(uint8_t OB_BOR) => to set the BOR Level 			 

   3. Once all needed Option Bytes to be programmed are correctly written, call the
      FLASH_OB_Launch() function to launch the Option Bytes programming process.
     
     @note When changing the IWDG mode from HW to SW or from SW to HW, a system 
           reset is needed to make the change effective.  

   4. Call the FLASH_OB_Lock() function to disable the FLASH option control register
      access (recommended to protect the Option Bytes against possible unwanted operations)
    
@endverbatim
  * @{
  */

/**
  * @brief  Unlocks the FLASH Option Control Registers access.
  * @param  None
  * @retval None
  */
void FLASH_OB_Unlock(void)
{
  if((FLASH->OPTCR & FLASH_OPTCR_OPTLOCK) != RESET)
  {
    /* Authorizes the Option Byte register programming */
    FLASH->OPTKEYR = FLASH_OPT_KEY1;
    FLASH->OPTKEYR = FLASH_OPT_KEY2;
  }  
}

/**
  * @brief  Locks the FLASH Option Control Registers access.
  * @param  None
  * @retval None
  */
void FLASH_OB_Lock(void)
{
  /* Set the OPTLOCK Bit to lock the FLASH Option Byte Registers access */
  FLASH->OPTCR |= FLASH_OPTCR_OPTLOCK;
}

/**
  * @brief  Enables or disables the write protection of the desired sectors
  * @param  OB_WRP: specifies the sector(s) to be write protected or unprotected.
  *          This parameter can be one of the following values:
  *            @arg OB_WRP: A value between OB_WRP_Sector0 and OB_WRP_Sector11                      
  *            @arg OB_WRP_Sector_All
  * @param  Newstate: new state of the Write Protection.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None  
  */
void FLASH_OB_WRPConfig(uint32_t OB_WRP, FunctionalState NewState)
{ 
  FLASH_Status status = FLASH_COMPLETE;
  
  /* Check the parameters */
  assert_param(IS_OB_WRP(OB_WRP));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
    
  status = FLASH_WaitForLastOperation();

  if(status == FLASH_COMPLETE)
  { 
    if(NewState != DISABLE)
    {
      *(__IO uint16_t*)OPTCR_BYTE2_ADDRESS &= (~OB_WRP);
    }
    else
    {
      *(__IO uint16_t*)OPTCR_BYTE2_ADDRESS |= (uint16_t)OB_WRP;
    }
  }
}

/**
  * @brief  Sets the read protection level.
  * @param  OB_RDP: specifies the read protection level.
  *          This parameter can be one of the following values:
  *            @arg OB_RDP_Level_0: No protection
  *            @arg OB_RDP_Level_1: Read protection of the memory
  *            @arg OB_RDP_Level_2: Full chip protection
  *   
  * !!!Warning!!! When enabling OB_RDP level 2 it's no more possible to go back to level 1 or 0
  *    
  * @retval None
  */
void FLASH_OB_RDPConfig(uint8_t OB_RDP)
{
  FLASH_Status status = FLASH_COMPLETE;

  /* Check the parameters */
  assert_param(IS_OB_RDP(OB_RDP));

  status = FLASH_WaitForLastOperation();

  if(status == FLASH_COMPLETE)
  {
    *(__IO uint8_t*)OPTCR_BYTE1_ADDRESS = OB_RDP;

  }
}

/**
  * @brief  Programs the FLASH User Option Byte: IWDG_SW / RST_STOP / RST_STDBY.    
  * @param  OB_IWDG: Selects the IWDG mode
  *          This parameter can be one of the following values:
  *            @arg OB_IWDG_SW: Software IWDG selected
  *            @arg OB_IWDG_HW: Hardware IWDG selected
  * @param  OB_STOP: Reset event when entering STOP mode.
  *          This parameter  can be one of the following values:
  *            @arg OB_STOP_NoRST: No reset generated when entering in STOP
  *            @arg OB_STOP_RST: Reset generated when entering in STOP
  * @param  OB_STDBY: Reset event when entering Standby mode.
  *          This parameter  can be one of the following values:
  *            @arg OB_STDBY_NoRST: No reset generated when entering in STANDBY
  *            @arg OB_STDBY_RST: Reset generated when entering in STANDBY
  * @retval None
  */
void FLASH_OB_UserConfig(uint8_t OB_IWDG, uint8_t OB_STOP, uint8_t OB_STDBY)
{
  uint8_t optiontmp = 0xFF;
  FLASH_Status status = FLASH_COMPLETE; 

  /* Check the parameters */
  assert_param(IS_OB_IWDG_SOURCE(OB_IWDG));
  assert_param(IS_OB_STOP_SOURCE(OB_STOP));
  assert_param(IS_OB_STDBY_SOURCE(OB_STDBY));

  /* Wait for last operation to be completed */
  status = FLASH_WaitForLastOperation();
  
  if(status == FLASH_COMPLETE)
  { 
    /* Mask OPTLOCK, OPTSTRT and BOR_LEV bits */
    optiontmp =  (uint8_t)((*(__IO uint8_t *)OPTCR_BYTE0_ADDRESS) & (uint8_t)0x0F); 

    /* Update User Option Byte */
    *(__IO uint8_t *)OPTCR_BYTE0_ADDRESS = OB_IWDG | (uint8_t)(OB_STDBY | (uint8_t)(OB_STOP | ((uint8_t)optiontmp))); 
  }  
}

/**
  * @brief  Sets the BOR Level. 
  * @param  OB_BOR: specifies the Option Bytes BOR Reset Level.
  *          This parameter can be one of the following values:
  *            @arg OB_BOR_LEVEL3: Supply voltage ranges from 2.7 to 3.6 V
  *            @arg OB_BOR_LEVEL2: Supply voltage ranges from 2.4 to 2.7 V
  *            @arg OB_BOR_LEVEL1: Supply voltage ranges from 2.1 to 2.4 V
  *            @arg OB_BOR_OFF: Supply voltage ranges from 1.62 to 2.1 V
  * @retval None
  */
void FLASH_OB_BORConfig(uint8_t OB_BOR)
{
  /* Check the parameters */
  assert_param(IS_OB_BOR(OB_BOR));

  /* Set the BOR Level */
  *(__IO uint8_t *)OPTCR_BYTE0_ADDRESS &= (~FLASH_OPTCR_BOR_LEV);
  *(__IO uint8_t *)OPTCR_BYTE0_ADDRESS |= OB_BOR;

}

/**
  * @brief  Launch the option byte loading.
  * @param  None
  * @retval FLASH Status: The returned value can be: FLASH_BUSY, FLASH_ERROR_PROGRAM,
  *                       FLASH_ERROR_WRP, FLASH_ERROR_OPERATION or FLASH_COMPLETE.
  */
FLASH_Status FLASH_OB_Launch(void)
{
  FLASH_Status status = FLASH_COMPLETE;

  /* Set the OPTSTRT bit in OPTCR register */
  *(__IO uint8_t *)OPTCR_BYTE0_ADDRESS |= FLASH_OPTCR_OPTSTRT;

  /* Wait for last operation to be completed */
  status = FLASH_WaitForLastOperation();

  return status;
}

/**
  * @brief  Returns the FLASH User Option Bytes values.
  * @param  None
  * @retval The FLASH User Option Bytes values: IWDG_SW(Bit0), RST_STOP(Bit1)
  *         and RST_STDBY(Bit2).
  */
uint8_t FLASH_OB_GetUser(void)
{
  /* Return the User Option Byte */
  return (uint8_t)(FLASH->OPTCR >> 5);
}

/**
  * @brief  Returns the FLASH Write Protection Option Bytes value.
  * @param  None
  * @retval The FLASH Write Protection  Option Bytes value
  */
uint16_t FLASH_OB_GetWRP(void)
{
  /* Return the FLASH write protection Register value */
  return (*(__IO uint16_t *)(OPTCR_BYTE2_ADDRESS));
}

/**
  * @brief  Returns the FLASH Read Protection level.
  * @param  None
  * @retval FLASH ReadOut Protection Status:
  *           - SET, when OB_RDP_Level_1 or OB_RDP_Level_2 is set
  *           - RESET, when OB_RDP_Level_0 is set
  */
FlagStatus FLASH_OB_GetRDP(void)
{
  FlagStatus readstatus = RESET;

  if ((*(__IO uint8_t*)(OPTCR_BYTE1_ADDRESS) != (uint8_t)OB_RDP_Level_0))
  {
    readstatus = SET;
  }
  else
  {
    readstatus = RESET;
  }
  return readstatus;
}

/**
  * @brief  Returns the FLASH BOR level.
  * @param  None
  * @retval The FLASH BOR level:
  *           - OB_BOR_LEVEL3: Supply voltage ranges from 2.7 to 3.6 V
  *           - OB_BOR_LEVEL2: Supply voltage ranges from 2.4 to 2.7 V
  *           - OB_BOR_LEVEL1: Supply voltage ranges from 2.1 to 2.4 V
  *           - OB_BOR_OFF   : Supply voltage ranges from 1.62 to 2.1 V  
  */
uint8_t FLASH_OB_GetBOR(void)
{
  /* Return the FLASH BOR level */
  return (uint8_t)(*(__IO uint8_t *)(OPTCR_BYTE0_ADDRESS) & (uint8_t)0x0C);
}

/**
  * @}
  */

/** @defgroup FLASH_Group4 Interrupts and flags management functions
 *  @brief   Interrupts and flags management functions
 *
@verbatim   
 ===============================================================================
                  Interrupts and flags management functions
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Enables or disables the specified FLASH interrupts.
  * @param  FLASH_IT: specifies the FLASH interrupt sources to be enabled or disabled.
  *          This parameter can be any combination of the following values:
  *            @arg FLASH_IT_ERR: FLASH Error Interrupt
  *            @arg FLASH_IT_EOP: FLASH end of operation Interrupt
  * @retval None 
  */
void FLASH_ITConfig(uint32_t FLASH_IT, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FLASH_IT(FLASH_IT)); 
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  if(NewState != DISABLE)
  {
    /* Enable the interrupt sources */
    FLASH->CR |= FLASH_IT;
  }
  else
  {
    /* Disable the interrupt sources */
    FLASH->CR &= ~(uint32_t)FLASH_IT;
  }
}

/**
  * @brief  Checks whether the specified FLASH flag is set or not.
  * @param  FLASH_FLAG: specifies the FLASH flag to check.
  *          This parameter can be one of the following values:
  *            @arg FLASH_FLAG_EOP: FLASH End of Operation flag 
  *            @arg FLASH_FLAG_OPERR: FLASH operation Error flag 
  *            @arg FLASH_FLAG_WRPERR: FLASH Write protected error flag 
  *            @arg FLASH_FLAG_PGAERR: FLASH Programming Alignment error flag
  *            @arg FLASH_FLAG_PGPERR: FLASH Programming Parallelism error flag
  *            @arg FLASH_FLAG_PGSERR: FLASH Programming Sequence error flag
  *            @arg FLASH_FLAG_BSY: FLASH Busy flag
  * @retval The new state of FLASH_FLAG (SET or RESET).
  */
FlagStatus FLASH_GetFlagStatus(uint32_t FLASH_FLAG)
{
  FlagStatus bitstatus = RESET;
  /* Check the parameters */
  assert_param(IS_FLASH_GET_FLAG(FLASH_FLAG));

  if((FLASH->SR & FLASH_FLAG) != (uint32_t)RESET)
  {
    bitstatus = SET;
  }
  else
  {
    bitstatus = RESET;
  }
  /* Return the new state of FLASH_FLAG (SET or RESET) */
  return bitstatus; 
}

/**
  * @brief  Clears the FLASH's pending flags.
  * @param  FLASH_FLAG: specifies the FLASH flags to clear.
  *          This parameter can be any combination of the following values:
  *            @arg FLASH_FLAG_EOP: FLASH End of Operation flag 
  *            @arg FLASH_FLAG_OPERR: FLASH operation Error flag 
  *            @arg FLASH_FLAG_WRPERR: FLASH Write protected error flag 
  *            @arg FLASH_FLAG_PGAERR: FLASH Programming Alignment error flag 
  *            @arg FLASH_FLAG_PGPERR: FLASH Programming Parallelism error flag
  *            @arg FLASH_FLAG_PGSERR: FLASH Programming Sequence error flag
  * @retval None
  */
void FLASH_ClearFlag(uint32_t FLASH_FLAG)
{
  /* Check the parameters */
  assert_param(IS_FLASH_CLEAR_FLAG(FLASH_FLAG));
  
  /* Clear the flags */
  FLASH->SR = FLASH_FLAG;
}

/**
  * @brief  Returns the FLASH Status.
  * @param  None
  * @retval FLASH Status: The returned value can be: FLASH_BUSY, FLASH_ERROR_PROGRAM,
  *                       FLASH_ERROR_WRP, FLASH_ERROR_OPERATION or FLASH_COMPLETE.
  */
FLASH_Status FLASH_GetStatus(void)
{
  FLASH_Status flashstatus = FLASH_COMPLETE;
  
  if((FLASH->SR & FLASH_FLAG_BSY) == FLASH_FLAG_BSY) 
  {
    flashstatus = FLASH_BUSY;
  }
  else 
  {  
    if((FLASH->SR & FLASH_FLAG_WRPERR) != (uint32_t)0x00)
    { 
      flashstatus = FLASH_ERROR_WRP;
    }
    else 
    {
      if((FLASH->SR & (uint32_t)0xEF) != (uint32_t)0x00)
      {
        flashstatus = FLASH_ERROR_PROGRAM; 
      }
      else
      {
        if((FLASH->SR & FLASH_FLAG_OPERR) != (uint32_t)0x00)
        {
          flashstatus = FLASH_ERROR_OPERATION;
        }
        else
        {
          flashstatus = FLASH_COMPLETE;
        }
      }
    }
  }
  /* Return the FLASH Status */
  return flashstatus;
}

/**
  * @brief  Waits for a FLASH operation to complete.
  * @param  None
  * @retval FLASH Status: The returned value can be: FLASH_BUSY, FLASH_ERROR_PROGRAM,
  *                       FLASH_ERROR_WRP, FLASH_ERROR_OPERATION or FLASH_COMPLETE.
  */
FLASH_Status FLASH_WaitForLastOperation(void)
{ 
  __IO FLASH_Status status = FLASH_COMPLETE;
   
  /* Check for the FLASH Status */
  status = FLASH_GetStatus();

  /* Wait for the FLASH operation to complete by polling on BUSY flag to be reset.
     Even if the FLASH operation fails, the BUSY flag will be reset and an error
     flag will be set */
  while(status == FLASH_BUSY)
  {
    status = FLASH_GetStatus();
  }
  /* Return the operation status */
  return status;
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
